import { h, defineComponent } from 'vue'
import type { FunctionalComponent } from 'vue'
import * as mdiJs from '@mdi/js'
import {
  mdiHome,
  mdiMenu,
  mdiMenuOpen,
  mdiMagnify,
  mdiTools,
  mdiCog,
  mdiWhiteBalanceSunny,
  mdiMoonWaningCrescent,
  mdiFolder,
  mdiTag,
  mdiDotsHorizontal,
  mdiFolderOpenOutline,
  mdiPencil,
  mdiViewGrid,
  mdiViewList,
  mdiArrowLeft,
  mdiDownload,
  mdiPlus,
  mdiDelete,
  mdiFormatListBulleted,
  mdiClose,
  mdiCheckCircle,
  mdiPlay,
  mdiPause,
  mdiChevronDown,
  mdiChevronRight,
  mdiChevronLeft,
  mdiChevronUp,
  mdiStar,
  mdiStarOutline,
  mdiStarHalfFull,
  mdiInformation,
  mdiAlertCircle,
  mdiCheckboxMarked,
  mdiCheckboxBlankOutline,
  mdiMinusBox,
  mdiCircle,
  mdiArrowUp,
  mdiArrowDown,
  mdiArrowRight,
  mdiRadioboxMarked,
  mdiRadioboxBlank,
  mdiUnfoldMoreHorizontal,
  mdiPaperclip,
  mdiMinus,
  mdiCalendar,
  mdiCached,
  mdiPageFirst,
  mdiPageLast,
  mdiEyedropper,
  mdiCloudUpload,
  mdiPalette,
  mdiAppleKeyboardCommand,
  mdiAppleKeyboardControl,
  mdiAppleKeyboardShift,
  mdiAppleKeyboardOption,
  mdiKeyboardSpace,
  mdiKeyboardReturn,
  mdiBackspace,
  mdiFullscreen,
  mdiFullscreenExit,
  mdiVolumeHigh,
  mdiVolumeMedium,
  mdiVolumeLow,
  mdiVolumeVariantOff,
  mdiCloseCircle,
} from '@mdi/js'

function kebabToCamel(kebab: string): string {
  return kebab.replace(/-([a-z])/g, (_, c) => c.toUpperCase())
}

const iconMap: Record<string, string> = {
  mdiHome,
  mdiMenu,
  mdiMenuOpen,
  mdiMagnify,
  mdiTools,
  mdiCog,
  mdiWhiteBalanceSunny,
  mdiMoonWaningCrescent,
  mdiFolder,
  mdiTag,
  mdiDotsHorizontal,
  mdiFolderOpenOutline,
  mdiPencil,
  mdiViewGrid,
  mdiViewList,
  mdiArrowLeft,
  mdiDownload,
  mdiPlus,
  mdiDelete,
  mdiFormatListBulleted,
  mdiClose,
  mdiCheckCircle,
  mdiPlay,
  mdiPause,
  mdiChevronDown,
  mdiChevronRight,
  mdiChevronLeft,
  mdiChevronUp,
  mdiStar,
  mdiStarOutline,
  mdiStarHalfFull,
  mdiInformation,
  mdiAlertCircle,
  mdiCheckboxMarked,
  mdiCheckboxBlankOutline,
  mdiMinusBox,
  mdiCircle,
  mdiArrowUp,
  mdiArrowDown,
  mdiArrowRight,
  mdiRadioboxMarked,
  mdiRadioboxBlank,
  mdiUnfoldMoreHorizontal,
  mdiPaperclip,
  mdiMinus,
  mdiCalendar,
  mdiCached,
  mdiPageFirst,
  mdiPageLast,
  mdiEyedropper,
  mdiCloudUpload,
  mdiPalette,
  mdiAppleKeyboardCommand,
  mdiAppleKeyboardControl,
  mdiAppleKeyboardShift,
  mdiAppleKeyboardOption,
  mdiKeyboardSpace,
  mdiKeyboardReturn,
  mdiBackspace,
  mdiFullscreen,
  mdiFullscreenExit,
  mdiVolumeHigh,
  mdiVolumeMedium,
  mdiVolumeLow,
  mdiVolumeVariantOff,
  mdiCloseCircle,
}

export const aliases: Record<string, string> = {
  collapse: iconMap.mdiChevronUp,
  complete: iconMap.mdiCheckCircle,
  cancel: iconMap.mdiCloseCircle,
  close: iconMap.mdiClose,
  delete: iconMap.mdiCloseCircle,
  clear: iconMap.mdiCloseCircle,
  success: iconMap.mdiCheckCircle,
  info: iconMap.mdiInformation,
  warning: iconMap.mdiAlertCircle,
  error: iconMap.mdiCloseCircle,
  prev: iconMap.mdiChevronLeft,
  next: iconMap.mdiChevronRight,
  checkboxOn: iconMap.mdiCheckboxMarked,
  checkboxOff: iconMap.mdiCheckboxBlankOutline,
  checkboxIndeterminate: iconMap.mdiMinusBox,
  delimiter: iconMap.mdiCircle,
  sortAsc: iconMap.mdiArrowUp,
  sortDesc: iconMap.mdiArrowDown,
  expand: iconMap.mdiChevronDown,
  menu: iconMap.mdiMenu,
  subgroup: iconMap.mdiChevronDown,
  dropdown: iconMap.mdiChevronDown,
  radioOn: iconMap.mdiRadioboxMarked,
  radioOff: iconMap.mdiRadioboxBlank,
  edit: iconMap.mdiPencil,
  ratingEmpty: iconMap.mdiStarOutline,
  ratingFull: iconMap.mdiStar,
  ratingHalf: iconMap.mdiStarHalfFull,
  loading: iconMap.mdiCached,
  first: iconMap.mdiPageFirst,
  last: iconMap.mdiPageLast,
  unfold: iconMap.mdiUnfoldMoreHorizontal,
  file: iconMap.mdiPaperclip,
  plus: iconMap.mdiPlus,
  minus: iconMap.mdiMinus,
  calendar: iconMap.mdiCalendar,
  eyeDropper: iconMap.mdiEyedropper,
  upload: iconMap.mdiCloudUpload,
  color: iconMap.mdiPalette,
  command: iconMap.mdiAppleKeyboardCommand,
  ctrl: iconMap.mdiAppleKeyboardControl,
  space: iconMap.mdiKeyboardSpace,
  shift: iconMap.mdiAppleKeyboardShift,
  alt: iconMap.mdiAppleKeyboardOption,
  enter: iconMap.mdiKeyboardReturn,
  arrowup: iconMap.mdiArrowUp,
  arrowdown: iconMap.mdiArrowDown,
  arrowleft: iconMap.mdiArrowLeft,
  arrowright: iconMap.mdiArrowRight,
  backspace: iconMap.mdiBackspace,
  play: iconMap.mdiPlay,
  pause: iconMap.mdiPause,
  fullscreen: iconMap.mdiFullscreen,
  fullscreenExit: iconMap.mdiFullscreenExit,
  volumeHigh: iconMap.mdiVolumeHigh,
  volumeMedium: iconMap.mdiVolumeMedium,
  volumeLow: iconMap.mdiVolumeLow,
  volumeOff: iconMap.mdiVolumeVariantOff,
  search: iconMap.mdiMagnify,
  treeviewCollapse: iconMap.mdiChevronDown,
  treeviewExpand: iconMap.mdiChevronRight,
  tableGroupCollapse: iconMap.mdiChevronDown,
  tableGroupExpand: iconMap.mdiChevronRight,
}

function resolveIconPath(iconName: string): string | undefined {
  if (/^[Mm]\d/.test(iconName) || iconName.startsWith('svg:')) {
    return iconName.startsWith('svg:') ? iconName.slice(4) : iconName
  }
  if (iconName.startsWith('$')) {
    return aliases[iconName.slice(1)]
  }
  if (iconName.startsWith('mdi:')) {
    const name = iconName.slice(4)
    return iconMap[name] ?? iconMap[`mdi${kebabToCamel(name)}`]
  }
  if (iconName.startsWith('mdi-')) {
    const camelKey = `mdi${kebabToCamel(iconName.slice(4)).replace(/^[a-z]/, c => c.toUpperCase())}`
    return iconMap[camelKey]
  }
  return undefined
}

function renderSvg(tag: any, path: string, attrs: Record<string, unknown>) {
  return h(tag, { ...attrs, style: null }, [
    h('svg', {
      class: 'v-icon__svg',
      xmlns: 'http://www.w3.org/2000/svg',
      viewBox: '0 0 24 24',
      role: 'img',
      'aria-hidden': 'true',
    }, [
      h('path', { d: path }),
    ]),
  ])
}

export const MdiIcon = defineComponent({
  name: 'MdiIcon',
  inheritAttrs: false,
  props: {
    icon: [String, Function, Object, Array],
    tag: { type: [String, Object, Function], required: true },
  },
  setup(props, { attrs }) {
    return () => {
      const iconName = typeof props.icon === 'string' ? props.icon : ''
      // Try our icon map first
      const path = iconName ? resolveIconPath(iconName) : undefined
      if (path) {
        return renderSvg(props.tag, path, attrs as Record<string, unknown>)
      }
      // Fallback: try @mdi/js namespace for any mdi-xxx icon
      if (iconName.startsWith('mdi-')) {
        const camelKey = `mdi${kebabToCamel(iconName.slice(4)).replace(/^[a-z]/, c => c.toUpperCase())}`
        const fallbackPath = (mdiJs as Record<string, unknown>)[camelKey] as string | undefined
        if (fallbackPath && typeof fallbackPath === 'string') {
          return renderSvg(props.tag, fallbackPath, attrs as Record<string, unknown>)
        }
      }
      // For non-string icons (components, arrays), pass through as VNode
      if (typeof props.icon !== 'string') {
        return h(props.tag as any, { ...attrs, style: null })
      }
      // Last resort: render tag with icon name as class
      return h(props.tag as any, { ...attrs, class: [attrs.class as string, props.icon] })
    }
  },
})

export const mdi = {
  component: MdiIcon,
}
