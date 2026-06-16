# Custom Components Library

A custom component library that mimics JDS (Jio Design System) with full control over styling and behavior.

## 📦 Available Components

- ✅ **Text** - Typography component
- ✅ **Button/ActionButton** - Button components
- ✅ **Avatar** - User avatar component
- ✅ **Divider** - Separator component

## 🚀 Quick Start

### Before (JDS):
```javascript
import { Text, ActionButton, Avatar, Divider } from "@jds/core";
```

### After (Custom):
```javascript
import { Text, ActionButton, Avatar, Divider } from "../custom-components";
```

That's it! The API is almost identical.

## 📖 Component API

### Text Component

```javascript
<Text 
  appearance="heading-m"      // heading-xxl to heading-xxs, body-l to body-xxs, body-*-bold
  color="primary-grey-100"    // primary-grey-100, primary-grey-80, primary_60, etc.
  className="custom-class"
  style={{}}
>
  Your text here
</Text>
```

### Button/ActionButton Component

```javascript
<ActionButton
  kind="primary"              // primary, secondary, tertiary, danger
  size="medium"               // small, medium, large
  label="Click Me"
  icon="ic_back"              // icon name or React element
  disabled={false}
  onClick={() => {}}
/>
```

### Avatar Component

```javascript
<Avatar
  kind="initials"             // initials, image, icon
  initials="AB"
  src={imageUrl}
  size="medium"               // small, medium, large, xtra-large
  onClick={() => {}}
/>
```

### Divider Component

```javascript
<Divider 
  variant="default"           // default, bold, dashed
  orientation="horizontal"    // horizontal, vertical
/>
```

## 🔄 Migration Steps

### Step 1: Update Profile.js (Example)

**Before:**
```javascript
import { Text, ActionButton, Avatar, Divider } from "@jds/core";
```

**After:**
```javascript
import { Text, ActionButton, Avatar, Divider } from "../custom-components";
```

### Step 2: Test the Component

Run your app and verify everything works as expected. The custom components have the same API, so no code changes needed!

### Step 3: Gradually Migrate Other Files

Repeat for each component file:
1. Update imports
2. Test functionality
3. Customize styling if needed

## 🎨 Customization

All styles are in separate CSS files. You can easily customize:

- `Text.css` - Typography styles
- `Button.css` - Button styles
- `Avatar.css` - Avatar styles
- `Divider.css` - Divider styles

### Example Customization:

Want to change primary button color? Edit `Button.css`:

```css
.custom-button-primary {
  background: #your-color;  /* Change this */
  color: #ffffff;
  border: 1px solid #your-color;
}
```

## 📝 Adding More Components

Need InputField, Modal, or other components? Follow this pattern:

1. Create `ComponentName.js`
2. Create `ComponentName.css`
3. Export in `index.js`
4. Follow JDS API structure

## 🔧 Extending Components

You can extend any component:

```javascript
import { Button } from "../custom-components";

const MyCustomButton = (props) => {
  return (
    <Button 
      {...props} 
      className={`my-custom-class ${props.className}`}
    />
  );
};
```

## 💡 Benefits

✅ **Full Control** - Customize every aspect
✅ **No Dependencies** - No JDS library needed
✅ **Same API** - Minimal code changes
✅ **Easy Maintenance** - All styles in one place
✅ **Performance** - Lightweight components
✅ **Type Safety** - Add PropTypes/TypeScript easily

## 🐛 Troubleshooting

**Q: Component doesn't look exactly like JDS?**
A: Adjust the CSS files to match JDS styling more closely.

**Q: Missing a component?**
A: Create it following the same pattern as existing components.

**Q: Icons not working?**
A: Icons need to be implemented separately. You can use React Icons or custom SVGs.

## 📚 Next Steps

1. Test the migration with Profile.js
2. Create additional components as needed
3. Customize colors and styles to match your design
4. Share feedback and improvements!

Happy coding! 🎉

