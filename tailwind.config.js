const defaultTheme = require('tailwindcss/defaultTheme')
const colors = require('tailwindcss/colors')

module.exports = {
  corePlugins: {
    preflight: true
  },
  plugins: [],
  future: {
    removeDeprecatedGapUtilities: true,
    purgeLayersByDefault: true,
    defaultLineHeights: true,
    standardFontWeights: true
  },
  purge: [
    './src/**/*.clj',
    './src/**/*.cljs'
  ],
  theme: {
    extend: {
      colors: {
        teal: colors.teal
      },
      borderColor: {
        teal: colors.teal
      },
      fontFamily: {
        sans: [
          'Source Serif Pro',
          ...defaultTheme.fontFamily.sans,
        ],
        mono: [
          'Fira Code',
          ...defaultTheme.fontFamily.mono,
        ]
      }
    }
  },
  variants: {
    extend: {
      borderWidth: ['last'],
    }
  }
}
