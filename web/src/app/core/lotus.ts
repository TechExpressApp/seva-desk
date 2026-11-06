import { Component, input } from '@angular/core';

/** Line-drawn lotus used on the sign-in screen and as a quiet motif. Inherits currentColor. */
@Component({
  selector: 'sd-lotus',
  template: `
    <svg viewBox="0 0 100 100" [attr.width]="size()" [attr.height]="size()" aria-hidden="true">
      <g fill="none" stroke="currentColor" stroke-width="2.2" stroke-linejoin="round">
        <path d="M50 18 C60 34 60 54 50 72 C40 54 40 34 50 18Z" />
        <path d="M50 72 C38 62 28 48 26 32 C40 36 48 50 50 72Z" />
        <path d="M50 72 C62 62 72 48 74 32 C60 36 52 50 50 72Z" />
        <path d="M50 72 C34 70 20 60 12 46 C28 46 42 56 50 72Z" />
        <path d="M50 72 C66 70 80 60 88 46 C72 46 58 56 50 72Z" />
        <path d="M22 80 Q50 70 78 80" />
        <path d="M30 87 Q50 80 70 87" />
      </g>
    </svg>
  `,
  styles: `:host { display: inline-flex; line-height: 0; }`,
})
export class Lotus {
  readonly size = input(64);
}
