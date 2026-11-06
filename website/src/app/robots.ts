import { MetadataRoute } from "next";
import { APP_CONFIG } from "@/config/app.config";

export const dynamic = "force-static";

export default function robots(): MetadataRoute.Robots {
  return {
    rules: {
      userAgent: "*",
      allow: "/",
    },
    sitemap: `${APP_CONFIG.seo.siteUrl}/sitemap.xml`,
  };
}
