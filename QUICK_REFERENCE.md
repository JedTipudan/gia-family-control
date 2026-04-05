# Quick Reference: Google Family Link Design System

## 🎨 Colors

### Primary Colors
```xml
<color name="google_blue">#4285F4</color>
<color name="google_red">#EA4335</color>
<color name="google_yellow">#FBBC04</color>
<color name="google_green">#34A853</color>
```

### Background Colors
```xml
<color name="bg_primary">#F8F9FA</color>      <!-- App background -->
<color name="bg_secondary">#FFFFFF</color>    <!-- Card background -->
```

### Status Colors
```xml
<color name="success">#34A853</color>         <!-- Green -->
<color name="success_bg">#E6F4EA</color>      <!-- Light green -->

<color name="danger">#EA4335</color>          <!-- Red -->
<color name="danger_bg">#FCE8E6</color>       <!-- Light red -->

<color name="info">#4285F4</color>            <!-- Blue -->
<color name="info_bg">#E8F0FE</color>         <!-- Light blue -->

<color name="warning">#FBBC04</color>         <!-- Yellow -->
<color name="warning_bg">#FEF7E0</color>      <!-- Light yellow -->
```

## 📏 Dimensions

### Card Styling
```xml
app:cardCornerRadius="24dp"
app:cardElevation="2dp"
android:layout_margin="16dp"
android:padding="24dp"
```

### Button Sizing
```xml
android:layout_height="56dp"    <!-- Standard button -->
android:layout_height="64dp"    <!-- Large button (SOS) -->
app:cornerRadius="16dp"
```

### Spacing
```xml
<!-- Padding inside cards -->
android:padding="24dp"

<!-- Margins between cards -->
android:layout_margin="16dp"

<!-- Spacing between elements -->
android:layout_marginBottom="16dp"
```

## 🔘 Button Styles

### Filled Button (Primary Action)
```xml
<com.google.android.material.button.MaterialButton
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:text="Primary Action"
    android:textAllCaps="false"
    app:backgroundTint="@color/google_blue"
    app:cornerRadius="16dp"/>
```

### Tonal Button (Secondary Action)
```xml
<com.google.android.material.button.MaterialButton
    style="@style/Widget.Material3.Button.TonalButton"
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:text="Secondary Action"
    android:textAllCaps="false"
    app:backgroundTint="@color/info_bg"
    android:textColor="@color/google_blue"
    app:cornerRadius="16dp"/>
```

### Outlined Button (Tertiary Action)
```xml
<com.google.android.material.button.MaterialButton
    style="@style/Widget.Material3.Button.OutlinedButton"
    android:layout_width="match_parent"
    android:layout_height="56dp"
    android:text="Tertiary Action"
    android:textAllCaps="false"
    app:strokeColor="@color/border_primary"
    android:textColor="@color/text_secondary"
    app:cornerRadius="16dp"/>
```

## 📦 Card Template

```xml
<com.google.android.material.card.MaterialCardView
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    app:cardCornerRadius="24dp"
    app:cardElevation="2dp"
    app:cardBackgroundColor="@color/white"
    android:layout_margin="16dp">

    <LinearLayout
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:orientation="vertical"
        android:padding="24dp">

        <!-- Card content here -->

    </LinearLayout>
</com.google.android.material.card.MaterialCardView>
```

## 📝 Typography

### Headers
```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Header"
    android:textSize="18sp"
    android:textStyle="bold"
    android:textColor="@color/text_primary"/>
```

### Body Text
```xml
<TextView
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:text="Body text"
    android:textSize="14sp"
    android:textColor="@color/text_secondary"/>
```

### Button Text
```xml
android:textSize="14sp"
android:textStyle="bold"
android:textAllCaps="false"
```

## 🎯 Common Patterns

### Status Card with Icon
```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal"
    android:gravity="center_vertical">

    <ImageView
        android:layout_width="48dp"
        android:layout_height="48dp"
        android:src="@drawable/icon"
        android:layout_marginEnd="16dp"/>

    <LinearLayout
        android:layout_width="0dp"
        android:layout_height="wrap_content"
        android:layout_weight="1"
        android:orientation="vertical">

        <TextView
            android:text="Title"
            android:textSize="18sp"
            android:textStyle="bold"
            android:textColor="@color/text_primary"/>

        <TextView
            android:text="Subtitle"
            android:textSize="14sp"
            android:textColor="@color/text_secondary"
            android:layout_marginTop="4dp"/>

    </LinearLayout>

</LinearLayout>
```

### Two-Column Button Layout
```xml
<LinearLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="horizontal">

    <MaterialButton
        android:layout_width="0dp"
        android:layout_height="56dp"
        android:layout_weight="1"
        android:text="Left"
        android:layout_marginEnd="8dp"/>

    <MaterialButton
        android:layout_width="0dp"
        android:layout_height="56dp"
        android:layout_weight="1"
        android:text="Right"
        android:layout_marginStart="8dp"/>

</LinearLayout>
```

## 🎨 Color Usage Guide

### When to Use Each Color:

**Blue (Primary)**
- Main actions (Pair, Start)
- Info messages
- Primary buttons

**Green (Success)**
- Unlock actions
- Success states
- Positive feedback
- QR scan button

**Red (Danger)**
- Lock actions
- Emergency/SOS
- Critical warnings
- Delete actions

**Yellow (Warning)**
- Pending states
- Warnings
- Attention needed

**Gray (Neutral)**
- Secondary actions
- Unpair/Cancel
- Disabled states

## 📱 Toolbar Template

```xml
<com.google.android.material.appbar.MaterialToolbar
    android:id="@+id/toolbar"
    android:layout_width="match_parent"
    android:layout_height="64dp"
    android:background="@color/white"
    android:elevation="2dp"
    app:title="Family Link"
    app:titleTextColor="@color/text_primary"
    app:navigationIcon="@drawable/ic_menu"/>
```

## 🔍 Text Input Template

```xml
<com.google.android.material.textfield.TextInputLayout
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:hint="Hint text"
    style="@style/Widget.Material3.TextInputLayout.OutlinedBox"
    app:boxCornerRadiusTopStart="16dp"
    app:boxCornerRadiusTopEnd="16dp"
    app:boxCornerRadiusBottomStart="16dp"
    app:boxCornerRadiusBottomEnd="16dp">
    
    <com.google.android.material.textfield.TextInputEditText
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textColor="@color/text_primary"/>
        
</com.google.android.material.textfield.TextInputLayout>
```

---

## ✅ Checklist for New Screens

When creating new screens, ensure:

- [ ] Cards use 24dp corner radius
- [ ] Cards have 2dp elevation
- [ ] Padding is 20-24dp
- [ ] Margins are 16-20dp
- [ ] Buttons are 56-64dp height
- [ ] Button corners are 16dp
- [ ] Headers are 18-24sp bold
- [ ] Body text is 14sp
- [ ] Colors match Google palette
- [ ] Tonal buttons for secondary actions
- [ ] Icons included in buttons
- [ ] textAllCaps="false" on buttons

---

**Quick Tip**: Copy these templates when creating new layouts to maintain consistency! 🎨
