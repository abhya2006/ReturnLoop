/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{ts,tsx}'],
  theme: {
    extend: {
      colors: {
        jet: 'rgb(var(--jet) / <alpha-value>)',
        cerulean: {
          DEFAULT: 'rgb(var(--cerulean) / <alpha-value>)',
          light: 'rgb(var(--cerulean-light) / <alpha-value>)',
          dark: 'rgb(var(--cerulean-dark) / <alpha-value>)',
          muted: 'rgb(var(--cerulean-muted) / <alpha-value>)',
        },
        rose: {
          DEFAULT: 'rgb(var(--rose) / <alpha-value>)',
          light: 'rgb(var(--rose-light) / <alpha-value>)',
          dark: 'rgb(var(--rose-dark) / <alpha-value>)',
        },
        cyan: 'rgb(var(--cyan) / <alpha-value>)',
        violet: 'rgb(var(--violet) / <alpha-value>)',
        surface: {
          base: 'rgb(var(--surface-base) / <alpha-value>)',
          elevated: 'rgb(var(--surface-elevated) / <alpha-value>)',
          hover: 'rgb(var(--surface-hover) / <alpha-value>)',
        },
        edge: {
          DEFAULT: 'rgb(var(--edge) / <alpha-value>)',
          strong: 'rgb(var(--edge-strong) / <alpha-value>)',
        },
        ink: {
          primary: 'rgb(var(--ink-primary) / <alpha-value>)',
          secondary: 'rgb(var(--ink-secondary) / <alpha-value>)',
          muted: 'rgb(var(--ink-muted) / <alpha-value>)',
        },
        ok: 'rgb(var(--ok) / <alpha-value>)',
        warn: 'rgb(var(--warn) / <alpha-value>)',
        danger: 'rgb(var(--danger) / <alpha-value>)',
      },
      fontFamily: {
        sans: ['Inter', 'system-ui', '-apple-system', 'Segoe UI', 'Roboto', 'sans-serif'],
        mono: ['JetBrains Mono', 'ui-monospace', 'SFMono-Regular', 'monospace'],
      },
      borderRadius: {
        card: '14px',
        panel: '18px',
      },
      boxShadow: {
        glow: '0 0 0 1px rgba(0,140,255,0.4), 0 0 30px -6px rgba(0,140,255,0.6)',
        'glow-rose': '0 0 0 1px rgba(255,45,149,0.5), 0 0 30px -6px rgba(255,45,149,0.7)',
        panel: '0 18px 40px -24px rgba(0,0,0,0.9)',
      },
      backgroundImage: {
        'brand-gradient': 'linear-gradient(135deg, #00e6ff 0%, #8c5aff 50%, #ff2d95 100%)',
        'hero-sheen':
          'radial-gradient(1000px 340px at 8% -20%, rgba(0,140,255,0.35), transparent 60%), radial-gradient(760px 300px at 96% 0%, rgba(255,45,149,0.22), transparent 62%)',
      },
      keyframes: {
        'fade-up': {
          '0%': { opacity: '0', transform: 'translateY(10px)' },
          '100%': { opacity: '1', transform: 'translateY(0)' },
        },
        float: {
          '0%, 100%': { transform: 'translateY(0px) translateZ(60px)' },
          '50%': { transform: 'translateY(-20px) translateZ(60px)' },
        },
        'float-delayed': {
          '0%, 100%': { transform: 'translateY(0px) translateZ(70px)' },
          '50%': { transform: 'translateY(-15px) translateZ(70px)' },
        },
        'float-slow': {
          '0%, 100%': { transform: 'translateY(0px) translateZ(65px)' },
          '50%': { transform: 'translateY(-25px) translateZ(65px)' },
        },
        orbit: {
          '0%': { transform: 'translate(-50%, -50%) rotate(0deg)' },
          '100%': { transform: 'translate(-50%, -50%) rotate(360deg)' },
        },
        'orbit-reverse': {
          '0%': { transform: 'translate(-50%, -50%) rotate(360deg)' },
          '100%': { transform: 'translate(-50%, -50%) rotate(0deg)' },
        },
        'orbit-slow': {
          '0%': { transform: 'translate(-50%, -50%) rotate(0deg)' },
          '100%': { transform: 'translate(-50%, -50%) rotate(360deg)' },
        },
        'spin-slow': {
          '0%': { transform: 'rotate(0deg) translateZ(10px)' },
          '100%': { transform: 'rotate(360deg) translateZ(10px)' },
        },
        'spin-reverse': {
          '0%': { transform: 'rotate(360deg) translateZ(20px)' },
          '100%': { transform: 'rotate(0deg) translateZ(20px)' },
        },
        shimmer: {
          '0%': { backgroundPosition: '-460px 0' },
          '100%': { backgroundPosition: '460px 0' },
        },
      },
      animation: {
        'fade-up': 'fade-up 600ms ease-out both',
        float: 'float 6s ease-in-out infinite',
        'float-delayed': 'float-delayed 6s ease-in-out infinite 2s',
        'float-slow': 'float-slow 8s ease-in-out infinite 4s',
        orbit: 'orbit 20s linear infinite',
        'orbit-reverse': 'orbit-reverse 25s linear infinite',
        'orbit-slow': 'orbit-slow 30s linear infinite',
        'spin-slow': 'spin-slow 40s linear infinite',
        'spin-reverse': 'spin-reverse 35s linear infinite',
        shimmer: 'shimmer 1.4s linear infinite',
      },
    },
  },
  plugins: [],
};
