"use client";

import { useState, useEffect } from "react";
import Image from "next/image";
import Link from "next/link";
import { Download, Menu, X, ArrowUpRight, Play } from "lucide-react";
import { APP_CONFIG } from "@/config/app.config";
import ThemeToggle from "@/components/ThemeToggle";
import { useDownload } from "@/context/DownloadContext";

export default function Navbar() {
  const [mobileMenuOpen, setMobileMenuOpen] = useState(false);
  const [scrolled, setScrolled] = useState(false);
  const { downloadState, progress, startDownload, openInstallDialog } = useDownload();

  useEffect(() => {
    let ticking = false;
    const handleScroll = () => {
      if (!ticking) {
        window.requestAnimationFrame(() => {
          setScrolled(window.scrollY > 20);
          ticking = false;
        });
        ticking = true;
      }
    };
    window.addEventListener("scroll", handleScroll, { passive: true });
    return () => window.removeEventListener("scroll", handleScroll);
  }, []);

  const handleDownloadAction = () => {
    if (downloadState === "downloaded") {
      openInstallDialog();
    } else {
      startDownload();
      const hero = document.querySelector("main");
      if (hero) hero.scrollIntoView({ behavior: "smooth" });
    }
  };

  const navLinks = [
    { name: "Overview", href: "#overview" },
    { name: "Capabilities", href: "#features" },
    { name: "Screenshots", href: "#screenshots" },
    { name: "Specifications", href: "#download" },
    { name: "Installation", href: "#installation" },
    { name: "Releases", href: "#releases" },
    { name: "FAQ", href: "#faq" },
  ];

  return (
    <header
      className={`sticky top-0 z-50 w-full transition-colors duration-150 ${
        scrolled
          ? "bg-[#F7F7F2]/95 dark:bg-[#111714]/95 backdrop-blur-sm border-b border-[#E2E5DD] dark:border-[#26372F]"
          : "bg-[#F7F7F2] dark:bg-[#111714] border-b border-[#E2E5DD] dark:border-[#26372F]"
      }`}
    >
      {/* Topmost Horizontal Progress Line across the viewport while downloading */}
      {downloadState === "downloading" && (
        <div className="absolute top-0 left-0 right-0 h-1 bg-[#E2E5DD] dark:bg-[#26372F] z-[60] overflow-hidden">
          <div
            className="h-full bg-[#173D32] dark:bg-[#C8D8B9] transition-all duration-100 ease-out"
            style={{ width: `${progress}%` }}
          />
        </div>
      )}

      <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8">
        <div className="flex items-center justify-between h-16 sm:h-20">

          {/* Brand Logo & Product Name (Version removed from top section) */}
          <Link
            href="/"
            className="flex items-center gap-3 group focus:outline-none"
          >
            <div className="relative w-8 h-8 sm:w-9 sm:h-9 overflow-hidden rounded border border-[#E2E5DD] dark:border-[#26372F] bg-white dark:bg-[#18221D] flex items-center justify-center">
              <Image
                src={APP_CONFIG.app.logoPath}
                alt={`${APP_CONFIG.app.name} logo`}
                width={36}
                height={36}
                className="object-cover"
                priority
              />
            </div>
            <div className="flex items-baseline gap-2">
              <span className="font-semibold text-lg sm:text-xl tracking-tight text-[#202521] dark:text-[#F3F6F1]">
                {APP_CONFIG.app.name}
              </span>
            </div>
          </Link>

          {/* Desktop Navigation Links */}
          <nav className="hidden lg:flex items-center gap-6 xl:gap-8">
            {navLinks.map((link) => (
              <a
                key={link.name}
                href={link.href}
                className="text-xs font-mono uppercase tracking-wider text-[#777D76] dark:text-[#8E9B93] hover:text-[#173D32] dark:hover:text-[#C8D8B9] transition-colors"
              >
                {link.name}
              </a>
            ))}
          </nav>

          {/* Right Action Items */}
          <div className="flex items-center gap-2.5 sm:gap-3">
            <Link
              href="/privacy"
              className="hidden sm:inline-block text-xs font-mono uppercase tracking-wider text-[#777D76] dark:text-[#8E9B93] hover:text-[#173D32] dark:hover:text-[#C8D8B9] transition-colors px-2 py-1"
            >
              Privacy
            </Link>

            {/* Night Mode Toggle */}
            <ThemeToggle />

            {/* Distinctive Download / Open App CTA */}
            <button
              onClick={handleDownloadAction}
              disabled={downloadState === "downloading"}
              type="button"
              className={`inline-flex items-center gap-2 px-4 py-2 sm:px-5 sm:py-2.5 rounded text-xs font-semibold uppercase tracking-wider transition-all focus:outline-none ${
                downloadState === "downloaded"
                  ? "text-[#F7F7F2] bg-[#173D32] hover:bg-[#315E4C] dark:bg-[#C8D8B9] dark:text-[#111714] dark:hover:bg-[#DCE7D1] shadow-sm ring-1 ring-[#C8D8B9]"
                  : downloadState === "downloading"
                  ? "text-[#F7F7F2] bg-[#315E4C] dark:bg-[#C8D8B9]/80 dark:text-[#111714] cursor-wait"
                  : "text-[#F7F7F2] bg-[#173D32] hover:bg-[#315E4C] dark:bg-[#C8D8B9] dark:text-[#111714] dark:hover:bg-[#DCE7D1]"
              }`}
            >
              {downloadState === "downloading" ? (
                <>
                  <div className="w-3.5 h-3.5 border-2 border-current border-t-transparent rounded-full animate-spin" />
                  <span>{progress}%</span>
                </>
              ) : downloadState === "downloaded" ? (
                <>
                  <Play className="w-3.5 h-3.5 fill-current text-[#C8D8B9] dark:text-[#111714]" />
                  <span>Install APK</span>
                </>
              ) : (
                <>
                  <Download className="w-3.5 h-3.5 text-[#C8D8B9] dark:text-[#111714]" />
                  <span>Download APK</span>
                </>
              )}
            </button>

            {/* Mobile Menu Trigger */}
            <button
              onClick={() => setMobileMenuOpen(!mobileMenuOpen)}
              type="button"
              className="lg:hidden flex items-center justify-center w-8 h-8 sm:w-9 sm:h-9 rounded border border-[#E2E5DD] dark:border-[#26372F] bg-white dark:bg-[#18221D] text-[#202521] dark:text-[#F3F6F1] hover:bg-[#EBF0E6] dark:hover:bg-[#202E28] transition-colors"
              aria-label="Toggle menu"
            >
              {mobileMenuOpen ? (
                <X className="w-4 h-4" />
              ) : (
                <Menu className="w-4 h-4" />
              )}
            </button>
          </div>
        </div>
      </div>

      {/* Mobile Drawer Menu */}
      {mobileMenuOpen && (
        <div className="lg:hidden bg-[#F7F7F2] dark:bg-[#111714] border-b border-[#E2E5DD] dark:border-[#26372F] px-4 pt-3 pb-6 space-y-3">
          <div className="flex flex-col divide-y divide-[#E2E5DD] dark:divide-[#26372F]">
            {navLinks.map((link) => (
              <a
                key={link.name}
                href={link.href}
                onClick={() => setMobileMenuOpen(false)}
                className="py-3 text-xs font-mono uppercase tracking-wider text-[#202521] dark:text-[#F3F6F1] hover:text-[#173D32] dark:hover:text-[#C8D8B9] transition-colors flex items-center justify-between"
              >
                <span>{link.name}</span>
                <ArrowUpRight className="w-3.5 h-3.5 text-[#777D76] dark:text-[#8E9B93]" />
              </a>
            ))}
            <Link
              href="/privacy"
              onClick={() => setMobileMenuOpen(false)}
              className="py-3 text-xs font-mono uppercase tracking-wider text-[#202521] dark:text-[#F3F6F1] hover:text-[#173D32] dark:hover:text-[#C8D8B9] transition-colors flex items-center justify-between"
            >
              <span>Privacy Policy</span>
              <ArrowUpRight className="w-3.5 h-3.5 text-[#777D76] dark:text-[#8E9B93]" />
            </Link>
            <Link
              href="/terms"
              onClick={() => setMobileMenuOpen(false)}
              className="py-3 text-xs font-mono uppercase tracking-wider text-[#202521] dark:text-[#F3F6F1] hover:text-[#173D32] dark:hover:text-[#C8D8B9] transition-colors flex items-center justify-between"
            >
              <span>Terms of Service</span>
              <ArrowUpRight className="w-3.5 h-3.5 text-[#777D76] dark:text-[#8E9B93]" />
            </Link>
          </div>

          <div className="pt-3">
            <button
              onClick={() => {
                setMobileMenuOpen(false);
                handleDownloadAction();
              }}
              type="button"
              className="flex items-center justify-center gap-2 w-full px-4 py-3 rounded text-xs font-semibold uppercase tracking-wider text-[#F7F7F2] bg-[#173D32] hover:bg-[#315E4C] dark:bg-[#C8D8B9] dark:text-[#111714] dark:hover:bg-[#DCE7D1] transition-colors"
            >
              {downloadState === "downloaded" ? (
                <>
                  <Play className="w-4 h-4 fill-current text-[#C8D8B9] dark:text-[#111714]" />
                  <span>Install APK</span>
                </>
              ) : (
                <>
                  <Download className="w-4 h-4 text-[#C8D8B9] dark:text-[#111714]" />
                  <span>Download APK</span>
                </>
              )}
            </button>
          </div>
        </div>
      )}
    </header>
  );
}
