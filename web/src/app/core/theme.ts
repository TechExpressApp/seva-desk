import { definePreset, palette } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

/** PrimeNG theme for a temple: Aura with the temple's primary colour expanded into a full 50–950 palette. */
export function sevaTheme(primaryColor: string) {
  return {
    preset: definePreset(Aura, { semantic: { primary: palette(primaryColor) } }),
    options: { darkModeSelector: '.sd-dark' },
  };
}
