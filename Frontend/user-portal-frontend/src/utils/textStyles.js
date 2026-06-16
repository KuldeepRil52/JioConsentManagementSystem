/**
 * Inline text styles (sizes, weights, colors) using the OS UI font stack.
 */
export const FONT_FAMILY_STACK =
  'system-ui, -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, "Helvetica Neue", Arial, sans-serif';

const SYSTEM_UI = FONT_FAMILY_STACK;

const COLORS = {
  "primary-grey-100": "#141414",
  "primary-grey-80": "rgba(0, 0, 0, 0.65)",
  "primary-60": "#0a2885",
  "primary-background": "#ffffff",
};

const TYPO = {
  "heading-s": {
    fontFamily: SYSTEM_UI,
    fontSize: 30,
    lineHeight: "30px",
    fontWeight: 900,
    letterSpacing: "-0.9px",
  },
  "heading-xs": {
    fontFamily: SYSTEM_UI,
    fontSize: 22,
    lineHeight: "26px",
    fontWeight: 900,
    letterSpacing: "-0.66px",
  },
  "heading-xxs": {
    fontFamily: SYSTEM_UI,
    fontSize: 15,
    lineHeight: "20px",
    fontWeight: 900,
    letterSpacing: "-0.45px",
  },
  "body-s": {
    fontFamily: SYSTEM_UI,
    fontSize: 15,
    lineHeight: "22px",
    fontWeight: 500,
    letterSpacing: "-0.075px",
  },
  "body-s-bold": {
    fontFamily: SYSTEM_UI,
    fontSize: 15,
    lineHeight: "22px",
    fontWeight: 700,
    letterSpacing: "-0.075px",
  },
  "body-xs": {
    fontFamily: SYSTEM_UI,
    fontSize: 14,
    lineHeight: "20px",
    fontWeight: 500,
    letterSpacing: "-0.07px",
  },
  "body-xs-bold": {
    fontFamily: SYSTEM_UI,
    fontSize: 14,
    lineHeight: "20px",
    fontWeight: 700,
    letterSpacing: "-0.07px",
  },
  button: {
    fontFamily: SYSTEM_UI,
    fontSize: 15,
    lineHeight: "22px",
    fontWeight: 700,
    letterSpacing: "-0.075px",
  },
};

function normalizeColorKey(color) {
  if (color == null) return "primary-grey-100";
  return String(color).replace(/_/g, "-");
}

export function textStyle(appearance, color) {
  const typo = TYPO[appearance] || TYPO["body-xs"];
  const colorKey = normalizeColorKey(color);
  const c = COLORS[colorKey] || COLORS["primary-grey-100"];
  return {
    margin: 0,
    padding: 0,
    ...typo,
    color: c,
    textDecoration: "none",
    textTransform: "none",
  };
}
