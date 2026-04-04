# Linear Dark Mode Design System

Applied to Gia Family Control web dashboard and Android app.

## Design Principles

- **Ultra-modern dark aesthetic** - Deep blacks with subtle elevation
- **Aggressive letter spacing** - Tight tracking (-0.01em to -0.02em) for modern feel
- **Indigo-violet accent** - #5E5CE6 primary color with hover/pressed states
- **Minimal borders** - 1px borders with #2A2A2A for subtle separation
- **Zero elevation** - Flat design with borders instead of shadows
- **Inter Variable font** - Professional typography with OpenType features

## Color System

### Backgrounds
- `bg-primary`: #0D0D0D - Main background
- `bg-secondary`: #161616 - Secondary surfaces
- `bg-tertiary`: #1A1A1A - Tertiary surfaces
- `bg-elevated`: #1F1F1F - Cards and elevated elements
- `bg-overlay`: #252525 - Overlays and modals

### Borders
- `border-primary`: #2A2A2A - Main borders
- `border-secondary`: #333333 - Secondary borders
- `border-focus`: #5E5CE6 - Focus states

### Text
- `text-primary`: #FFFFFF - Primary text
- `text-secondary`: #9CA3AF - Secondary text
- `text-tertiary`: #6B7280 - Tertiary text
- `text-disabled`: #4B5563 - Disabled text

### Accent (Indigo-Violet)
- `accent-primary`: #5E5CE6 - Primary accent
- `accent-hover`: #7371EF - Hover state
- `accent-pressed`: #4845D2 - Pressed state
- `accent-subtle`: rgba(94, 92, 230, 0.12) - Subtle backgrounds

### Status Colors
- `success`: #10B981 - Success states
- `danger`: #EF4444 - Error/danger states
- `warning`: #F59E0B - Warning states

## Typography

### Font Family
- **Primary**: Inter Variable
- **Fallback**: -apple-system, BlinkMacSystemFont, sans-serif
- **Monospace**: For code/technical content

### Letter Spacing
- **Tight**: -0.02em (headings)
- **Normal**: -0.01em (body text)
- **Wide**: 0.01em (captions, labels)

### Font Sizes
- **H1**: 32px / 28sp
- **H2**: 24px / 20sp
- **H3**: 18px / 16sp
- **Body**: 14px / 14sp
- **Caption**: 11-13px / 11-13sp

### Font Weights
- **Regular**: 400
- **Medium**: 500
- **Semibold**: 600

## Spacing Scale

- `space-1`: 4px
- `space-2`: 8px
- `space-3`: 12px
- `space-4`: 16px
- `space-5`: 20px
- `space-6`: 24px
- `space-8`: 32px
- `space-10`: 40px
- `space-12`: 48px

## Border Radius

- `radius-sm`: 6px
- `radius-md`: 8px
- `radius-lg`: 12px
- `radius-xl`: 16px

## Components

### Cards
- Background: `bg-elevated`
- Border: 1px solid `border-primary`
- Radius: 10-12px
- Elevation: 0dp (no shadows)

### Buttons
- Height: 48dp (Android) / 40px (Web)
- Radius: 8px
- Font size: 13sp/px
- Letter spacing: -0.01em
- No text transform (preserve case)

### Inputs
- Background: `bg-secondary`
- Border: 1px solid `border-primary`
- Focus: `border-focus` with subtle glow
- Radius: 8px
- Padding: 12px 16px

### Toolbar/Header
- Background: `bg-elevated`
- Height: 56dp (Android) / 56px (Web)
- Border bottom: 1px solid `border-primary`
- No elevation

## Implementation

### Web Dashboard
- CSS variables in `linear-theme.css`
- Imported in `index.js`
- Applied to all components

### Android App
- Colors in `colors.xml`
- Theme in `themes.xml`
- Typography in `typography.xml`
- Applied to layouts

## OpenType Features

Inter Variable supports advanced typography:
- `cv02` - Open digits
- `cv03` - Curved r
- `cv04` - Open four
- `cv11` - Single-story a

Applied via `font-feature-settings: 'cv02', 'cv03', 'cv04', 'cv11'`
