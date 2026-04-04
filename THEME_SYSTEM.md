# Theme System - Dark/Light Mode

## Web Dashboard

### Dark Mode (Default)
- **Background**: `#08090a` (ultra dark)
- **Secondary**: `#0f1011` (sidebar)
- **Elevated**: `#16171a` (cards)
- **Accent**: `#5e6ad2` (indigo-blue)
- **Text**: `#f7f8f8` primary, `#8a8f98` tertiary
- **Borders**: `rgba(255,255,255,0.08)` subtle

### Light Mode (Stripe-inspired)
- **Background**: `#ffffff` (pure white)
- **Secondary**: `#f6f9fc` (soft blue-gray)
- **Elevated**: `#ffffff` (cards)
- **Accent**: `#533afd` (Stripe purple)
- **Text**: `#061b31` (deep navy), `#64748d` (slate)
- **Borders**: `#e5edf5` (soft blue)
- **Shadow**: `rgba(50,50,93,0.25)` (blue-tinted)

### Features
- Toggle button in sidebar (☀️/🌙 icon)
- Persists to localStorage
- Smooth transitions (0.2s)
- CSS variables for all colors
- Works across all components

## Android App (Coming Soon)

### Dark Mode (Stripe-based)
Will implement Stripe's dark theme:
- Deep navy backgrounds
- Purple accents
- Blue-tinted shadows
- Weight 300 typography

### Light Mode
Stripe's signature light theme:
- Pure white backgrounds
- Deep navy text (#061b31)
- Stripe purple (#533afd)
- Blue-tinted shadows

## Implementation

### Web
1. ThemeContext provides `isDark` and `toggleTheme()`
2. CSS variables in `:root[data-theme="dark"]` and `:root[data-theme="light"]`
3. All components use `var(--color-name)`
4. Theme persists to localStorage

### Android (TODO)
1. Create `values-night/colors.xml` for dark theme
2. Use Stripe color palette
3. Add theme toggle in settings
4. Persist to SharedPreferences
