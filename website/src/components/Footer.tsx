import Image from "next/image";
import Link from "next/link";
import { ShieldCheck, Mail, Send, ArrowUpRight } from "lucide-react";
import { APP_CONFIG } from "@/config/app.config";

export default function Footer() {
  const currentYear = new Date().getFullYear();

  return (
    <footer className="bg-[#173D32] dark:bg-[#0B1511] text-[#F7F7F2] dark:text-[#F3F6F1] border-t border-[#315E4C] dark:border-[#1E332A] transition-colors">
      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 sm:py-20">

        {/* Top Architectural Grid */}
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-12 gap-10 pb-16 border-b border-[#315E4C] dark:border-[#1E332A]">

          {/* Brand Philosophy (Span 5) */}
          <div className="lg:col-span-5 space-y-4">
            <Link href="/" className="flex items-center gap-3">
              <div className="relative w-8 h-8 rounded border border-[#315E4C] dark:border-[#1E332A] bg-white overflow-hidden flex items-center justify-center">
                <Image
                  src={APP_CONFIG.app.logoPath}
                  alt={`${APP_CONFIG.app.name} logo`}
                  width={32}
                  height={32}
                  className="object-cover"
                />
              </div>
              <span className="font-semibold text-xl tracking-tight text-white">
                {APP_CONFIG.app.name}
              </span>
            </Link>

            <p className="text-sm text-[#C8D8B9] leading-relaxed max-w-sm font-normal">
              Direct digital distribution platform for water logistics, inventory management, and route delivery. Cryptographically signed production packages delivered directly to Android devices.
            </p>

            <div className="pt-2 flex items-center gap-3 text-xs font-mono text-[#C8D8B9]/80">
              <span className="flex items-center gap-1.5">
                <ShieldCheck className="w-3.5 h-3.5 text-[#C8D8B9]" />
                <span>SHA-256 Release Verified</span>
              </span>
              <span>·</span>
              <span>v{APP_CONFIG.release.version} Production</span>
            </div>
          </div>

          {/* Quick Navigation (Span 3) */}
          <div className="lg:col-span-3 space-y-3">
            <div className="text-[11px] font-mono uppercase tracking-widest text-[#C8D8B9]">
              Index
            </div>
            <ul className="space-y-2 text-xs font-mono uppercase tracking-wider text-white/80">
              <li>
                <a href="#overview" className="hover:text-white transition-colors">
                  Overview
                </a>
              </li>
              <li>
                <a href="#features" className="hover:text-white transition-colors">
                  Capabilities
                </a>
              </li>
              <li>
                <a href="#screenshots" className="hover:text-white transition-colors">
                  Screenshots
                </a>
              </li>
              <li>
                <a href="#download" className="hover:text-white transition-colors">
                  Download APK
                </a>
              </li>
              <li>
                <a href="#installation" className="hover:text-white transition-colors">
                  Installation
                </a>
              </li>
              <li>
                <a href="#releases" className="hover:text-white transition-colors">
                  Release Notes
                </a>
              </li>
              <li>
                <a href="#faq" className="hover:text-white transition-colors">
                  FAQ
                </a>
              </li>
            </ul>
          </div>

          {/* Direct Support & External Links (Span 4) */}
          <div className="lg:col-span-4 space-y-4">
            <div className="text-[11px] font-mono uppercase tracking-widest text-[#C8D8B9]">
              Support &amp; Channels
            </div>

            <div className="space-y-2.5 text-xs font-mono">
              <a
                href={`mailto:${APP_CONFIG.links.supportEmail}`}
                className="flex items-center justify-between p-3 rounded border border-[#315E4C] dark:border-[#1E332A] hover:bg-[#315E4C]/40 dark:hover:bg-[#1E332A]/50 transition-colors text-white"
              >
                <span className="flex items-center gap-2">
                  <Mail className="w-3.5 h-3.5 text-[#C8D8B9]" />
                  <span>{APP_CONFIG.links.supportEmail}</span>
                </span>
                <ArrowUpRight className="w-3.5 h-3.5 text-[#C8D8B9]" />
              </a>

              <a
                href={APP_CONFIG.links.githubRepo}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center justify-between p-3 rounded border border-[#315E4C] dark:border-[#1E332A] hover:bg-[#315E4C]/40 dark:hover:bg-[#1E332A]/50 transition-colors text-white"
              >
                <span className="flex items-center gap-2">
                  <svg className="w-3.5 h-3.5 fill-current text-[#C8D8B9]" viewBox="0 0 24 24">
                    <path fillRule="evenodd" clipRule="evenodd" d="M12 2C6.477 2 2 6.484 2 12.017c0 4.425 2.865 8.18 6.839 9.504.5.092.682-.217.682-.483 0-.237-.008-.868-.013-1.703-2.782.605-3.369-1.343-3.369-1.343-.454-1.158-1.11-1.466-1.11-1.466-.908-.62.069-.608.069-.608 1.003.07 1.53 1.032 1.53 1.032.892 1.53 2.341 1.088 2.91.832.092-.647.35-1.088.636-1.338-2.22-.253-4.555-1.113-4.555-4.951 0-1.093.39-1.988 1.029-2.688-.103-.253-.446-1.272.098-2.65 0 0 .84-.27 2.75 1.026A9.564 9.564 0 0112 6.844c.85.004 1.705.115 2.504.337 1.909-1.296 2.747-1.027 2.747-1.027.546 1.379.202 2.398.1 2.651.64.7 1.028 1.595 1.028 2.688 0 3.848-2.339 4.695-4.566 4.943.359.309.678.92.678 1.855 0 1.338-.012 2.419-.012 2.747 0 .268.18.58.688.482A10.019 10.019 0 0022 12.017C22 6.484 17.522 2 12 2z" />
                  </svg>
                  <span>Source Repository</span>
                </span>
                <ArrowUpRight className="w-3.5 h-3.5 text-[#C8D8B9]" />
              </a>

              <a
                href={APP_CONFIG.links.telegramSupport}
                target="_blank"
                rel="noopener noreferrer"
                className="flex items-center justify-between p-3 rounded border border-[#315E4C] dark:border-[#1E332A] hover:bg-[#315E4C]/40 dark:hover:bg-[#1E332A]/50 transition-colors text-white"
              >
                <span className="flex items-center gap-2">
                  <Send className="w-3.5 h-3.5 text-[#C8D8B9]" />
                  <span>Telegram Community</span>
                </span>
                <ArrowUpRight className="w-3.5 h-3.5 text-[#C8D8B9]" />
              </a>
            </div>
          </div>

        </div>

        {/* Bottom Grounded Bar */}
        <div className="pt-8 flex flex-col sm:flex-row items-center justify-between gap-4 text-xs font-mono text-[#C8D8B9]/80">
          <div>
            © {currentYear} {APP_CONFIG.app.name}. Built with architectural precision.
          </div>
          <div className="flex items-center gap-6">
            <Link href="/privacy" className="hover:text-white transition-colors">
              Privacy Policy
            </Link>
            <Link href="/terms" className="hover:text-white transition-colors">
              Terms of Use
            </Link>
            <a href="#download" className="hover:text-white transition-colors">
              APK Download (v{APP_CONFIG.release.version})
            </a>
          </div>
        </div>

      </div>
    </footer>
  );
}
