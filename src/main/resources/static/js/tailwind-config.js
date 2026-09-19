/**
 * MotoRent Đà Nẵng - Tailwind Configuration
 * Shared across all pages (Customer, Staff, Admin)
 * Source: Stitch "Coastal Midnight Velocity" Design System
 */
tailwind.config = {
  darkMode: "class",
  theme: {
    extend: {
      colors: {
        // === BRAND COLORS (từ Stitch Design System) ===
        "primary-container": "#f97316",     // Cam CTA chính
        "inverse-primary": "#9d4300",       // Cam đậm
        "primary": "#ffb690",               // Cam nhạt
        "on-primary": "#552100",
        "on-primary-container": "#582200",
        "primary-fixed": "#ffdbca",
        "primary-fixed-dim": "#ffb690",
        "on-primary-fixed": "#341100",
        "on-primary-fixed-variant": "#783200",

        // === SURFACE (Nền navy đậm) ===
        "surface": "#051424",
        "surface-dim": "#051424",
        "surface-bright": "#2c3a4c",
        "surface-container-lowest": "#010f1f",
        "surface-container-low": "#0d1c2d",
        "surface-container": "#122131",
        "surface-container-high": "#1c2b3c",
        "surface-container-highest": "#273647",
        "surface-variant": "#273647",
        "surface-tint": "#ffb690",

        // === TEXT ===
        "on-surface": "#d4e4fa",
        "on-surface-variant": "#e0c0b1",
        "on-background": "#d4e4fa",
        "inverse-surface": "#d4e4fa",
        "inverse-on-surface": "#233143",
        "background": "#051424",

        // === SECONDARY ===
        "secondary": "#c3c6d3",
        "on-secondary": "#2c303a",
        "secondary-container": "#454954",
        "on-secondary-container": "#b5b8c5",
        "secondary-fixed": "#dfe2ef",
        "secondary-fixed-dim": "#c3c6d3",
        "on-secondary-fixed": "#181b25",
        "on-secondary-fixed-variant": "#434751",

        // === TERTIARY (Xanh lá thành công) ===
        "tertiary": "#4edea3",
        "on-tertiary": "#003824",
        "tertiary-container": "#00b07a",
        "on-tertiary-container": "#003b26",
        "tertiary-fixed": "#6ffbbe",
        "tertiary-fixed-dim": "#4edea3",
        "on-tertiary-fixed": "#002113",
        "on-tertiary-fixed-variant": "#005236",

        // === ERROR ===
        "error": "#ffb4ab",
        "on-error": "#690005",
        "error-container": "#93000a",
        "on-error-container": "#ffdad6",

        // === OUTLINE ===
        "outline": "#a78b7d",
        "outline-variant": "#584237",
      },
      borderRadius: {
        DEFAULT: "0.25rem",
        lg: "0.5rem",
        xl: "0.75rem",
        "2xl": "1rem",
        "3xl": "1.5rem",
        full: "9999px",
      },
      spacing: {
        "space-xs": "0.25rem",
        "space-sm": "0.5rem",
        "space-md": "1rem",
        "space-lg": "1.5rem",
        "space-xl": "2.5rem",
        "gutter-sm": "1rem",
        "gutter": "1.5rem",
        "margin-sm": "1rem",
        "margin": "2rem",
      },
      fontFamily: {
        sans: ['"Plus Jakarta Sans"', 'Inter', 'system-ui', '-apple-system', 'sans-serif'],
        display: ['"Plus Jakarta Sans"', "sans-serif"],
        serif: ['"Playfair Display"', 'Merriweather', 'Georgia', 'serif'],
      },
      fontSize: {
        "display-lg": ["56px", { lineHeight: "64px", letterSpacing: "-0.02em", fontWeight: "800" }],
        "display-lg-mobile": ["36px", { lineHeight: "44px", letterSpacing: "-0.02em", fontWeight: "800" }],
        "headline-lg": ["32px", { lineHeight: "40px", letterSpacing: "-0.015em", fontWeight: "700" }],
        "headline-lg-mobile": ["26px", { lineHeight: "34px", letterSpacing: "-0.01em", fontWeight: "700" }],
        "headline-md": ["24px", { lineHeight: "32px", fontWeight: "700" }],
        "headline-sm": ["20px", { lineHeight: "28px", fontWeight: "600" }],
        "body-lg": ["18px", { lineHeight: "28px", fontWeight: "400" }],
        "body-md": ["15px", { lineHeight: "24px", fontWeight: "400" }],
        "body-sm": ["13px", { lineHeight: "20px", fontWeight: "400" }],
        "label-lg": ["15px", { lineHeight: "20px", letterSpacing: "0.01em", fontWeight: "600" }],
        "label-md": ["13px", { lineHeight: "18px", letterSpacing: "0.02em", fontWeight: "600" }],
        "label-sm": ["11px", { lineHeight: "14px", letterSpacing: "0.05em", fontWeight: "700" }],
      },
      boxShadow: {
        "glow-orange": "0 0 35px -5px rgba(249, 115, 22, 0.35)",
        "glow-orange-sm": "0 4px 14px rgba(249, 115, 22, 0.25)",
        "glow-orange-lg": "0 6px 20px rgba(249, 115, 22, 0.45)",
        "card-glass": "0 20px 40px -15px rgba(0, 0, 0, 0.7)",
        "glass": "0 8px 32px -4px rgba(0, 0, 0, 0.5), 0 0 0 1px rgba(255, 255, 255, 0.08)",
      },
      backdropBlur: {
        xs: "4px",
      },
      animation: {
        "pulse-slow": "pulse 3s cubic-bezier(0.4, 0, 0.6, 1) infinite",
        "float": "float 3s ease-in-out infinite",
      },
      keyframes: {
        float: {
          "0%, 100%": { transform: "translateY(0)" },
          "50%": { transform: "translateY(-6px)" },
        }
      }
    }
  }
};
