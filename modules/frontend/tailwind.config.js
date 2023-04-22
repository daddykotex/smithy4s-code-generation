/** @type {import('tailwindcss').Config} */
export default {
  content: [
    "./index.html",
    "./target/scala-*/smithy4s-code-generation-frontend-*/*.js",
  ],
  theme: {
    extend: {},
  },
  plugins: [],
};
