const OPEN_TAG_DIALOG_EVENT = 'oj-open-tag-dialog'

export function emitOpenTagDialog() {
  window.dispatchEvent(new CustomEvent(OPEN_TAG_DIALOG_EVENT))
}

export function getOpenTagDialogEventName() {
  return OPEN_TAG_DIALOG_EVENT
}
