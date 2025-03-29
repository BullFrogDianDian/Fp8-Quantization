#!/bin/bash

# 确保verilog目录存在
mkdir -p verilog

# 使用sbt生成Verilog代码
echo "生成FloatMul8的Verilog代码..."
cd ..
sbt "runMain Multiple.FloatMul8Generator"

# 复制生成的Verilog到verilog目录
cp FloatMul8.v verilog/

# 进入verilog目录
cd verilog

# 使用iverilog编译testbench和设计
echo "编译测试代码..."
iverilog -o sim_fp8 FloatMul8.v FloatMul8_tb_simple.v

# 运行仿真
echo "运行仿真..."
vvp sim_fp8

echo "测试完成！" 