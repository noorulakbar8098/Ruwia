import Image from "next/image";
import Link from "next/link";
import {
  Download,
  ShieldCheck,
  Mail,
  Send,
  MessageSquare,
  Heart,
} from "lucide-react";
import { APP_CONFIG } from "@/config/app.config";

export default function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="border-t border-slate-200 dark:border-slate-800 bg-white dark:bg-slate-950 text-slate-600 dark:text-slate-400">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 sm:py-20">
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-10">
          
          {/* Brand & About Column */}
          <div className="lg:col-span-2 space-y-4">
            <Link href="/" className="flex items-center gap-3">
              <div className="relative w-9 h-9 rounded-xl overflow-hidden shadow-sm border border-emerald-500/30 bg-slate-900 flex items-center justify-center">
                <Image
                  src={APP_CONFIG.app.logoPath}
                  alt={`${APP_CONFIG.app.name} logo`}
                  width={36}
                  height={36}
                  className="object-cover"
                />
              </div>
              <span className="font-bold text-xl tracking-tight text-slate-900 dark:text-white">
                {APP_CONFIG.app.name}
              </span>
            </Link>

            <p className="text-sm text-slate-600 dark:text-slate-400 leading-relaxed max-w-sm">
              {APP_CONFIG.app.shortDescription} Download direct signed production APKs with zero third-party telemetry.
            </p>

            <div className="flex items-center gap-3 pt-2">
              <a
                href={APP_CONFIG.links.githubRepo}
                target="_blank"
                rel="noopener noreferrer"
                aria-label="GitHub Repository"
                className="w-9 h-9 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900 flex items-center justify-center text-slate-600 dark:text-slate-300 hover:text-emerald-500 hover:border-emerald-500/40 transition-colors"
              >
                <svg className="w-4 h-4 fill-current" viewBox="0 0 24 24" aria-hidden="true">
                  <path fillRule="evenodd" clipRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" />
                </svg>
              </a>

              <a
                href={APP_CONFIG.links.telegramSupport}
                target="_blank"
                rel="noopener noreferrer"
                aria-label="Telegram Support"
                className="w-9 h-9 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900 flex items-center justify-center text-slate-600 dark:text-slate-300 hover:text-emerald-500 hover:border-emerald-500/40 transition-colors"
              >
                <Send className="w-4 h-4" />
              </a>

              <a
                href={`mailto:${APP_CONFIG.links.supportEmail}`}
                aria-label="Email Support"
                className="w-9 h-9 rounded-xl border border-slate-200 dark:border-slate-800 bg-slate-50 dark:bg-slate-900 flex items-center justify-center text-slate-600 dark:text-slate-300 hover:text-emerald-500 hover:border-emerald-500/40 transition-colors"
              >
                <Mail className="w-4 h-4" />
              </a>
            </div>
          </div>

          {/* Quick Navigation Links */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-900 dark:text-white">
              Application
            </h4>
            <ul className="space-y-2 text-sm">
              <li>
                <a href="#features" className="hover:text-emerald-500 transition-colors">
                  Features & Capabilities
                </a>
              </li>
              <li>
                <a href="#screenshots" className="hover:text-emerald-500 transition-colors">
                  Mobile Screenshots
                </a>
              </li>
              <li>
                <a href="#download" className="hover:text-emerald-500 transition-colors">
                  Download Latest APK
                </a>
              </li>
              <li>
                <a href="#installation" className="hover:text-emerald-500 transition-colors">
                  Installation Guide
                </a>
              </li>
              <li>
                <a href="#releases" className="hover:text-emerald-500 transition-colors">
                  Release Notes
                </a>
              </li>
            </ul>
          </div>

          {/* Support & Community */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-900 dark:text-white">
              Support & Community
            </h4>
            <ul className="space-y-2 text-sm">
              <li>
                <a
                  href={`mailto:${APP_CONFIG.links.supportEmail}`}
                  className="hover:text-emerald-500 transition-colors"
                >
                  Email: {APP_CONFIG.links.supportEmail}
                </a>
              </li>
              <li>
                <a
                  href={APP_CONFIG.links.telegramSupport}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-emerald-500 transition-colors"
                >
                  Telegram Channel
                </a>
              </li>
              <li>
                <a
                  href={APP_CONFIG.links.githubIssues}
                  target="_blank"
                  rel="noopener noreferrer"
                  className="hover:text-emerald-500 transition-colors"
                >
                  Bug Reports & Feature Requests
                </a>
              </li>
              <li>
                <a href="#faq" className="hover:text-emerald-500 transition-colors">
                  FAQ & Troubleshooting
                </a>
              </li>
            </ul>
          </div>

          {/* Legal & Compliance */}
          <div className="space-y-3">
            <h4 className="text-xs font-bold uppercase tracking-wider text-slate-900 dark:text-white">
              Legal & Security
            </h4>
            <ul className="space-y-2 text-sm">
              <li>
                <Link href="/privacy" className="hover:text-emerald-500 transition-colors">
                  Privacy Policy
                </Link>
              </li>
              <li>
                <Link href="/terms" className="hover:text-emerald-500 transition-colors">
                  Terms of Service
                </Link>
              </li>
              <li>
                <span className="flex items-center gap-1.5 text-xs text-emerald-600 dark:text-emerald-400">
                  <ShieldCheck className="w-3.5 h-3.5 shrink-0" />
                  <span>SHA-256 Verified Builds</span>
                </span>
              </li>
            </ul>
          </div>

        </div>

        {/* Bottom Copyright & Disclaimer Bar */}
        <div className="mt-12 pt-8 border-t border-slate-200 dark:border-slate-800 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs text-slate-500">
          <div>
            © {currentYear} {APP_CONFIG.app.name}. All rights reserved. Distributed under official release key.
          </div>
          <div className="flex items-center gap-4">
            <Link href="/privacy" className="hover:underline">
              Privacy
            </Link>
            <span>•</span>
            <Link href="/terms" className="hover:underline">
              Terms
            </Link>
            <span>•</span>
            <a href="#download" className="hover:underline">
              Download v{APP_CONFIG.release.version}
            </a>
          </div>
        </div>

      </div>
    </footer>
  );
}
