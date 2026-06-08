import 'package:flutter/material.dart';
import 'package:liquid_glass_widgets/liquid_glass_widgets.dart';

class GlassPanel extends StatelessWidget {
  const GlassPanel({
    required this.child,
    super.key,
    this.padding = const EdgeInsets.all(16),
    this.margin,
    this.radius = 22,
    this.useOwnLayer = false,
  });

  final Widget child;
  final EdgeInsetsGeometry padding;
  final EdgeInsetsGeometry? margin;
  final double radius;
  final bool useOwnLayer;

  @override
  Widget build(BuildContext context) {
    return GlassContainer(
      padding: padding,
      margin: margin,
      shape: LiquidRoundedSuperellipse(borderRadius: radius),
      useOwnLayer: useOwnLayer,
      child: child,
    );
  }
}

class AppGlyph extends StatelessWidget {
  const AppGlyph({super.key, this.size = 44});

  final double size;

  @override
  Widget build(BuildContext context) {
    return GlassContainer(
      width: size,
      height: size,
      alignment: Alignment.center,
      shape: LiquidRoundedSuperellipse(borderRadius: size * 0.28),
      useOwnLayer: true,
      child: Text(
        'TG',
        style: Theme.of(context).textTheme.labelLarge?.copyWith(
          color: const Color(0xFF143C38),
          fontWeight: FontWeight.w800,
        ),
      ),
    );
  }
}
