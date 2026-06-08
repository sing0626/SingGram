import 'package:flutter/material.dart';

class LiquidBackdrop extends StatelessWidget {
  const LiquidBackdrop({super.key});

  @override
  Widget build(BuildContext context) {
    return const DecoratedBox(
      decoration: BoxDecoration(
        gradient: LinearGradient(
          begin: Alignment.topLeft,
          end: Alignment.bottomRight,
          colors: [Color(0xFFE8F8F5), Color(0xFFF8F3EA), Color(0xFFE9EDF8)],
        ),
      ),
      child: _BackdropRibbons(),
    );
  }
}

class _BackdropRibbons extends StatelessWidget {
  const _BackdropRibbons();

  @override
  Widget build(BuildContext context) {
    return CustomPaint(
      painter: _RibbonPainter(),
      child: const SizedBox.expand(),
    );
  }
}

class _RibbonPainter extends CustomPainter {
  @override
  void paint(Canvas canvas, Size size) {
    final tealPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = size.shortestSide * 0.16
      ..strokeCap = StrokeCap.round
      ..color = const Color(0x5546B6A8);

    final coralPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = size.shortestSide * 0.12
      ..strokeCap = StrokeCap.round
      ..color = const Color(0x44D66D58);

    final inkPaint = Paint()
      ..style = PaintingStyle.stroke
      ..strokeWidth = size.shortestSide * 0.10
      ..strokeCap = StrokeCap.round
      ..color = const Color(0x33556FB5);

    canvas.drawPath(
      Path()
        ..moveTo(-size.width * 0.10, size.height * 0.20)
        ..cubicTo(
          size.width * 0.24,
          size.height * 0.02,
          size.width * 0.46,
          size.height * 0.42,
          size.width * 1.10,
          size.height * 0.18,
        ),
      tealPaint,
    );

    canvas.drawPath(
      Path()
        ..moveTo(size.width * 0.08, size.height * 0.86)
        ..cubicTo(
          size.width * 0.30,
          size.height * 0.52,
          size.width * 0.76,
          size.height * 1.08,
          size.width * 1.12,
          size.height * 0.64,
        ),
      coralPaint,
    );

    canvas.drawPath(
      Path()
        ..moveTo(size.width * 0.02, size.height * 0.48)
        ..cubicTo(
          size.width * 0.30,
          size.height * 0.34,
          size.width * 0.58,
          size.height * 0.58,
          size.width * 0.98,
          size.height * 0.40,
        ),
      inkPaint,
    );
  }

  @override
  bool shouldRepaint(covariant CustomPainter oldDelegate) => false;
}
