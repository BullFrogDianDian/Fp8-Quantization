`timescale 1ns / 1ps

module FloatMul8_tb;
  reg clock;
  reg reset;
  reg [7:0] io_a;
  reg [7:0] io_b;
  wire [7:0] io_c;

  // 实例化待测模块
  FloatMul8 uut (
    .clock(clock),
    .reset(reset),
    .io_a(io_a),
    .io_b(io_b),
    .io_c(io_c)
  );

  // 时钟生成
  always #5 clock = ~clock;

  initial begin
    // 初始化
    clock = 0;
    reset = 1;
    io_a = 0;
    io_b = 0;
    #10;
    reset = 0;

    // 测试用例
    io_a = 8'b01000010; // 2.5 (E4M3 格式)
    io_b = 8'b00110000; // 0.5
    #10;
    $display("Test 1: 2.5 * 0.5 = %b (Expected: 00111010)", io_c);
    
    io_a = 8'b00100010; // 0.15625
    io_b = 8'b00110000; // 0.5
    #10;
    $display("Test 2: 0.15625 * 0.5 = %b (Expected: 00011010)", io_c);

    io_a = 8'b01001000; // 3.0
    io_b = 8'b01000000; // 2.0
    #10;
    $display("Test 3: 3.0 * 2.0 = %b (Expected: 01010000)", io_c);

    io_a = 8'b00000000; // 0.0
    io_b = 8'b01000010; // 2.5
    #10;
    $display("Test 4: 0.0 * 2.5 = %b (Expected: 00000000)", io_c);

    io_a = 8'b01000000; // 2.0
    io_b = 8'b01000010; // 2.5
    #10;
    $display("Test 5: 2.0 * 2.5 = %b (Expected: 01001010)", io_c);

    $finish;
  end
endmodule
