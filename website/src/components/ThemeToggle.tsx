"use client";

import { useEffect, useState } from "react";
import { Sun, Moon } from "lucide-react";

export default function ThemeToggle() {
  const [theme, setTheme] = useState<"dark" | "light">("light");
  const [mounted, setMounted] = useState(false);

  useEffect(() => {
    setMounted(true);
    const isDark = document.documentElement.classList.contains("dark");
    setTheme(isDark ? "dark" : "light");
  }, []);

  const toggleTheme = () => {
    if (theme === "dark") {
      setTheme("light");
      document.documentElement.classList.remove("dark");
      localStorage.setItem("ruwia-theme", "light");
    } else {
      setTheme("dark");
      document.documentElement.classList.add("dark");
      localStorage.setItem("ruwia-theme", "dark");
    }
  };

  if (!mounted) {
    return (
      <div className="w-8 h-8 rounded border border-[#E2E5DD] dark:border-[#26372F] bg-white dark:bg-[#18221D]" />
    );
  }

  return (
    <button
      onClick={toggleTheme}
      type="button"
      aria-label={theme === "dark" ? "Switch to day mode" : "Switch to night mode"}
      title={theme === "dark" ? "Day Mode" : "Night Mode"}
      className="flex items-center justify-center w-8 h-8 sm:w-9 sm:h-9 rounded border border-[#E2E5DD] dark:border-[#26372F] bg-white dark:bg-[#18221D] text-[#202521] dark:text-[#F3F6F1] hover:bg-[#EBF0E6] dark:hover:bg-[#202E28] hover:text-[#173D32] dark:hover:text-[#C8D8B9] transition-colors focus:outline-none"
    >
      {theme === "dark" ? (
        <Sun className="w-4 h-4 text-[#C8D8B9]" />
      ) : (
        <Moon className="w-4 h-4 text-[#202521]" />
      )}
    </button>
  );
}
