/*
 * LearnMate AI - ExecuTorch Synthetic PTE Mmap Runner
 *
 * Dedicated C++ verification runner for physical ARM64 Android devices (Redmi 9 Power).
 * Loads an ExecuTorch .pte artifact using Module::LoadMode::Mmap, verifies memory mapping,
 * executes forward pass with known inputs, verifies mathematical correctness, and reports
 * microsecond-level timing and /proc/self/status memory telemetry.
 */

#include <iostream>
#include <fstream>
#include <sstream>
#include <vector>
#include <string>
#include <chrono>
#include <cmath>
#include <iomanip>
#include <unistd.h>
#include <sys/resource.h>

#include <executorch/extension/module/module.h>
#include <executorch/extension/tensor/tensor.h>
#include <executorch/runtime/platform/runtime.h>

using executorch::extension::Module;
using executorch::extension::from_blob;
using executorch::extension::TensorPtr;
using executorch::runtime::EValue;
using executorch::runtime::Error;
using executorch::runtime::Result;

struct MemoryMetrics {
    long vm_peak_kb = 0;
    long vm_size_kb = 0;
    long vm_rss_kb = 0;
    long vm_data_kb = 0;
};

static MemoryMetrics get_proc_status_memory() {
    MemoryMetrics metrics;
    std::ifstream file("/proc/self/status");
    if (!file.is_open()) {
        return metrics;
    }
    std::string line;
    while (std::getline(file, line)) {
        if (line.compare(0, 7, "VmPeak:") == 0) {
            std::stringstream ss(line.substr(7));
            ss >> metrics.vm_peak_kb;
        } else if (line.compare(0, 7, "VmSize:") == 0) {
            std::stringstream ss(line.substr(7));
            ss >> metrics.vm_size_kb;
        } else if (line.compare(0, 6, "VmRSS:") == 0) {
            std::stringstream ss(line.substr(6));
            ss >> metrics.vm_rss_kb;
        } else if (line.compare(0, 7, "VmData:") == 0) {
            std::stringstream ss(line.substr(7));
            ss >> metrics.vm_data_kb;
        }
    }
    return metrics;
}

static void print_proc_maps_for_file(const std::string& filename) {
    std::cout << "--- /proc/self/maps entries matching '" << filename << "' ---" << std::endl;
    std::ifstream file("/proc/self/maps");
    if (!file.is_open()) {
        std::cout << "Could not open /proc/self/maps" << std::endl;
        return;
    }
    std::string line;
    bool found = false;
    while (std::getline(file, line)) {
        if (line.find(filename) != std::string::npos) {
            std::cout << "  " << line << std::endl;
            found = true;
        }
    }
    if (!found) {
        std::cout << "  [None found or mapped anonymously]" << std::endl;
    }
    std::cout << "--------------------------------------------------------" << std::endl;
}

int main(int argc, char** argv) {
    std::cout << "==========================================================" << std::endl;
    std::cout << "LearnMate AI - ExecuTorch Synthetic PTE Mmap Runner" << std::endl;
    std::cout << "Target: Android ARM64-v8a (Snapdragon 662 / Redmi 9 Power)" << std::endl;
    std::cout << "==========================================================" << std::endl;

    if (argc < 2) {
        std::cerr << "Usage: " << argv[0] << " <path_to_model.pte> [num_iterations]" << std::endl;
        return 1;
    }

    std::string pte_path = argv[1];
    int iterations = 1;
    if (argc >= 3) {
        iterations = std::atoi(argv[2]);
        if (iterations <= 0) iterations = 1;
    }

    std::cout << "[Runner] Model Path: " << pte_path << std::endl;
    std::cout << "[Runner] Iterations: " << iterations << std::endl;

    // Check file existence and size
    std::ifstream pte_file(pte_path, std::ios::binary | std::ios::ate);
    if (!pte_file.is_open()) {
        std::cerr << "[ERROR] Cannot open file: " << pte_path << std::endl;
        return 2;
    }
    std::streamsize file_size = pte_file.tellg();
    pte_file.close();
    std::cout << "[Runner] PTE File Size: " << file_size << " bytes (" 
              << std::fixed << std::setprecision(2) << (double)file_size / 1024.0 << " KB)" << std::endl;

    // Baseline memory
    MemoryMetrics mem_baseline = get_proc_status_memory();
    std::cout << "[Memory Baseline] VmSize: " << mem_baseline.vm_size_kb << " KB, VmRSS: " 
              << mem_baseline.vm_rss_kb << " KB, VmData: " << mem_baseline.vm_data_kb << " KB" << std::endl;

    // 1. Initialize Runtime
    executorch::runtime::runtime_init();

    // 2. Instantiate Module with LoadMode::Mmap
    std::cout << "[Runner] Creating Module with LoadMode::Mmap..." << std::endl;
    auto t_create_start = std::chrono::high_resolution_clock::now();
    Module module(pte_path, Module::LoadMode::Mmap);
    auto t_create_end = std::chrono::high_resolution_clock::now();
    auto create_us = std::chrono::duration_cast<std::chrono::microseconds>(t_create_end - t_create_start).count();
    std::cout << "[Runner] Module instantiated in: " << create_us << " us (" 
              << std::fixed << std::setprecision(3) << (double)create_us / 1000.0 << " ms)" << std::endl;

    // 3. Load Program
    std::cout << "[Runner] Loading Program (parse flatbuffer via mmap)..." << std::endl;
    auto t_load_start = std::chrono::high_resolution_clock::now();
    Error err = module.load();
    auto t_load_end = std::chrono::high_resolution_clock::now();
    auto load_us = std::chrono::duration_cast<std::chrono::microseconds>(t_load_end - t_load_start).count();

    if (err != Error::Ok) {
        std::cerr << "[ERROR] module.load() failed with error code: 0x" 
                  << std::hex << (uint32_t)err << std::dec << std::endl;
        return 3;
    }
    std::cout << "[Runner] Program loaded successfully in: " << load_us << " us (" 
              << std::fixed << std::setprecision(3) << (double)load_us / 1000.0 << " ms)" << std::endl;

    // Memory after load
    MemoryMetrics mem_after_load = get_proc_status_memory();
    std::cout << "[Memory Post-Load] VmSize: " << mem_after_load.vm_size_kb << " KB (delta: " 
              << (mem_after_load.vm_size_kb - mem_baseline.vm_size_kb) << " KB), VmRSS: " 
              << mem_after_load.vm_rss_kb << " KB (delta: " 
              << (mem_after_load.vm_rss_kb - mem_baseline.vm_rss_kb) << " KB)" << std::endl;

    // Inspect /proc/self/maps to verify mmap
    // Extract base filename from path
    std::string base_filename = pte_path;
    size_t last_slash = base_filename.find_last_of('/');
    if (last_slash != std::string::npos) {
        base_filename = base_filename.substr(last_slash + 1);
    }
    print_proc_maps_for_file(base_filename);

    // 4. Load Method "forward"
    std::cout << "[Runner] Loading method 'forward'..." << std::endl;
    auto t_method_start = std::chrono::high_resolution_clock::now();
    err = module.load_method("forward");
    auto t_method_end = std::chrono::high_resolution_clock::now();
    auto method_us = std::chrono::duration_cast<std::chrono::microseconds>(t_method_end - t_method_start).count();

    if (err != Error::Ok) {
        std::cerr << "[ERROR] module.load_method('forward') failed with error code: 0x" 
                  << std::hex << (uint32_t)err << std::dec << std::endl;
        return 4;
    }
    std::cout << "[Runner] Method 'forward' loaded in: " << method_us << " us (" 
              << std::fixed << std::setprecision(3) << (double)method_us / 1000.0 << " ms)" << std::endl;

    // 5. Prepare Test Inputs: A = [1.0, 2.0, 3.0, 4.0], B = [10.0, 20.0, 30.0, 40.0]
    float a_vals[4] = {1.0f, 2.0f, 3.0f, 4.0f};
    float b_vals[4] = {10.0f, 20.0f, 30.0f, 40.0f};
    const float expected[4] = {11.0f, 22.0f, 33.0f, 44.0f};

    std::vector<executorch::aten::SizesType> shape = {1, 4};
    TensorPtr a_tensor = from_blob(a_vals, shape);
    TensorPtr b_tensor = from_blob(b_vals, shape);

    std::vector<EValue> input_evalues;
    input_evalues.emplace_back(*a_tensor);
    input_evalues.emplace_back(*b_tensor);

    std::cout << "[Runner] Input A: [" << a_vals[0] << ", " << a_vals[1] << ", " << a_vals[2] << ", " << a_vals[3] << "]" << std::endl;
    std::cout << "[Runner] Input B: [" << b_vals[0] << ", " << b_vals[1] << ", " << b_vals[2] << ", " << b_vals[3] << "]" << std::endl;
    std::cout << "[Runner] Expected Output: [" << expected[0] << ", " << expected[1] << ", " << expected[2] << ", " << expected[3] << "]" << std::endl;

    // 6. Execute Forward Pass (Loop for warmup and iterations)
    std::vector<double> exec_latencies_us;
    exec_latencies_us.reserve(iterations);

    bool all_passed = true;
    for (int i = 0; i < iterations; ++i) {
        auto t_exec_start = std::chrono::high_resolution_clock::now();
        Result<std::vector<EValue>> exec_result = module.execute("forward", input_evalues);
        auto t_exec_end = std::chrono::high_resolution_clock::now();
        double exec_us = (double)std::chrono::duration_cast<std::chrono::nanoseconds>(t_exec_end - t_exec_start).count() / 1000.0;
        exec_latencies_us.push_back(exec_us);

        if (!exec_result.ok()) {
            std::cerr << "[ERROR] Execution failed on iteration " << (i + 1) << " with error code: 0x" 
                      << std::hex << (uint32_t)exec_result.error() << std::dec << std::endl;
            return 5;
        }

        std::vector<EValue> outputs = exec_result.get();
        if (outputs.size() < 1) {
            std::cerr << "[ERROR] Expected 1 output tensor, received: " << outputs.size() << std::endl;
            return 6;
        }

        executorch::aten::Tensor out_tensor = outputs[0].toTensor();
        const float* out_ptr = out_tensor.const_data_ptr<float>();

        if (i == 0 || i == iterations - 1) {
            std::cout << "[Run " << (i + 1) << "] Output: [" 
                      << out_ptr[0] << ", " << out_ptr[1] << ", " << out_ptr[2] << ", " << out_ptr[3] << "]"
                      << " | Latency: " << std::fixed << std::setprecision(1) << exec_us << " us" << std::endl;
        }

        // Numerical validation (epsilon = 1e-5)
        for (int k = 0; k < 4; ++k) {
            float diff = std::fabs(out_ptr[k] - expected[k]);
            if (diff > 1e-5f) {
                std::cerr << "[FAIL] Iteration " << (i + 1) << " output mismatch at [" << k << "]: expected " 
                          << expected[k] << ", got " << out_ptr[k] << ", diff: " << diff << std::endl;
                all_passed = false;
            }
        }
    }

    // Memory after execution
    MemoryMetrics mem_after_exec = get_proc_status_memory();
    std::cout << "[Memory Post-Exec] VmPeak: " << mem_after_exec.vm_peak_kb << " KB, VmSize: " 
              << mem_after_exec.vm_size_kb << " KB, VmRSS: " << mem_after_exec.vm_rss_kb << " KB, VmData: " 
              << mem_after_exec.vm_data_kb << " KB" << std::endl;

    // Execution Statistics
    double total_us = 0.0;
    double min_us = exec_latencies_us[0];
    double max_us = exec_latencies_us[0];
    for (double us : exec_latencies_us) {
        total_us += us;
        if (us < min_us) min_us = us;
        if (us > max_us) max_us = us;
    }
    double avg_us = total_us / iterations;

    std::cout << "==========================================================" << std::endl;
    std::cout << "BENCHMARK RESULTS SUMMARY" << std::endl;
    std::cout << "==========================================================" << std::endl;
    std::cout << "Model Load Time (Mmap):      " << load_us << " us (" 
              << std::fixed << std::setprecision(3) << (double)load_us / 1000.0 << " ms)" << std::endl;
    std::cout << "Method Load Time ('forward'): " << method_us << " us (" 
              << std::fixed << std::setprecision(3) << (double)method_us / 1000.0 << " ms)" << std::endl;
    std::cout << "Execution Latency (Avg):      " << std::fixed << std::setprecision(1) << avg_us << " us (" 
              << std::setprecision(3) << avg_us / 1000.0 << " ms)" << std::endl;
    std::cout << "Execution Latency (Min):      " << std::fixed << std::setprecision(1) << min_us << " us" << std::endl;
    std::cout << "Execution Latency (Max):      " << std::fixed << std::setprecision(1) << max_us << " us" << std::endl;
    std::cout << "Memory VmRSS Delta:           " << (mem_after_exec.vm_rss_kb - mem_baseline.vm_rss_kb) << " KB" << std::endl;
    std::cout << "Verification Status:          " << (all_passed ? "PASSED (100% Numerical Match)" : "FAILED") << std::endl;
    std::cout << "==========================================================" << std::endl;

    return all_passed ? 0 : 1;
}
