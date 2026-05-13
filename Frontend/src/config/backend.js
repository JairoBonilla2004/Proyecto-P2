export const API_BASE_URL='https://api.secureframe-gallery.com/api/v1';

export const BACKEND_ORIGIN = (() => {
  try {
    if (/^https?:\/\//i.test(API_BASE_URL)) {
      return new URL(API_BASE_URL).origin;
    }
  } catch {
    // ignore
  }

  // Fallback: assume same origin as the SPA.
  return window.location.origin;
})();

export const GOOGLE_OAUTH2_AUTH_URL = `${BACKEND_ORIGIN}/oauth2/authorization/google`;
