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
  purge: [],
  theme: {
    extend: {
      colors: {
        teal: colors.teal
      },
      fontFamily: {
        sans: [
          ...defaultTheme.fontFamily.sans,
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
