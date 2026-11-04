/** @type {import('tailwindcss').Config} */
export default {
  content: ["./index.html", "./src/**/*.{ts,tsx}"],
  theme: {
    extend: {
      colors: {
        cream: "#f4efe6",
        paper: "#fffaf4",
        ink: "#241c18",
        bark: "#5c4a3e",
        ember: "#c2410c",
        "ember-dark": "#9a3412",
        sage: "#3f6212",
        line: "#e7dccd"
      },
      fontFamily: {
        display: ["Fraunces", "Georgia", "serif"],
        sans: ["Outfit", "Segoe UI", "sans-serif"]
      },
      boxShadow: {
        card: "0 16px 40px rgba(36, 28, 24, 0.06)"
      }
    }
  },
  plugins: []
};
