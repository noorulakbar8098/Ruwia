import type { NextConfig } from "next";

// GitHub Pages project sites are served from /<repo>/. Set this env var in CI
// to build a prefixed export. Root deployments (Vercel/custom domain) leave it
// unset and keep the original root-relative URLs.
const basePath = process.env.NEXT_PUBLIC_BASE_PATH || undefined;

const nextConfig: NextConfig = {
  output: "export",
  basePath,
  images: {
    unoptimized: true,
  },
  trailingSlash: true,
};

export default nextConfig;
