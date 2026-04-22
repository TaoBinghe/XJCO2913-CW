export const LOCATION_ERROR_CODES = {
  PERMISSION_NOT_GRANTED: 'PERMISSION_NOT_GRANTED',
  PERMISSION_STILL_BLOCKED: 'PERMISSION_STILL_BLOCKED',
  PERMISSION_JUST_ENABLED: 'PERMISSION_JUST_ENABLED',
  LOCATION_UNAVAILABLE: 'LOCATION_UNAVAILABLE'
}

function createLocationError(code, originalError = null, extra = {}) {
  return {
    code,
    originalError,
    ...extra
  }
}

function getErrorMessage(error) {
  return String(error?.errMsg || error?.message || '').toLowerCase()
}

function looksLikePermissionError(error) {
  const message = getErrorMessage(error)
  return [
    'auth deny',
    'auth denied',
    'authorize no response',
    'permission denied',
    'system permission denied',
    'scope.userlocation'
  ].some(token => message.includes(token))
}

function showModal(options) {
  return new Promise((resolve) => {
    uni.showModal({
      ...options,
      success: (res) => resolve(!!res.confirm),
      fail: () => resolve(false)
    })
  })
}

function getLocation() {
  return new Promise((resolve, reject) => {
    uni.getLocation({
      type: 'gcj02',
      success: resolve,
      fail: reject
    })
  })
}

function authorizeLocation() {
  if (typeof uni.authorize !== 'function') {
    return Promise.resolve(true)
  }

  return new Promise((resolve, reject) => {
    uni.authorize({
      scope: 'scope.userLocation',
      success: () => resolve(true),
      fail: reject
    })
  })
}

function getLocationSettingState() {
  if (typeof uni.getSetting !== 'function') {
    return Promise.resolve(null)
  }

  return new Promise((resolve) => {
    uni.getSetting({
      success: (res) => resolve(res?.authSetting?.['scope.userLocation']),
      fail: () => resolve(null)
    })
  })
}

function openSettings() {
  if (typeof uni.openSetting !== 'function') {
    return Promise.resolve(null)
  }

  return new Promise((resolve) => {
    uni.openSetting({
      success: resolve,
      fail: () => resolve(null)
    })
  })
}

async function ensureLocationPermission(options) {
  const permissionState = await getLocationSettingState()

  if (permissionState === true) {
    return true
  }

  if (permissionState === false) {
    const code = await handlePermissionFlow(options)
    throw createLocationError(code, null, { handled: true })
  }

  if (typeof uni.authorize === 'function') {
    try {
      await authorizeLocation()
      return true
    } catch (error) {
      const code = await handlePermissionFlow(options)
      throw createLocationError(code, error, { handled: true })
    }
  }

  return true
}

async function handlePermissionFlow(options) {
  const {
    reasonTitle = 'Location permission needed',
    reasonContent = 'Please enable location permission to continue.',
    successHint = 'Location enabled. Please tap again.'
  } = options

  if (typeof uni.openSetting !== 'function') {
    await showModal({
      title: reasonTitle,
      content: `${reasonContent}\n\nPlease enable location permission in your app or system settings, then tap again.`,
      confirmText: 'OK',
      showCancel: false
    })
    return LOCATION_ERROR_CODES.PERMISSION_NOT_GRANTED
  }

  const confirmed = await showModal({
    title: reasonTitle,
    content: reasonContent,
    confirmText: 'Open Settings',
    cancelText: 'Cancel'
  })

  if (!confirmed) {
    return LOCATION_ERROR_CODES.PERMISSION_NOT_GRANTED
  }

  const settingResult = await openSettings()
  if (settingResult?.authSetting?.['scope.userLocation']) {
    uni.showToast({ title: successHint, icon: 'none' })
    return LOCATION_ERROR_CODES.PERMISSION_JUST_ENABLED
  }

  return LOCATION_ERROR_CODES.PERMISSION_STILL_BLOCKED
}

export async function getCurrentLocationWithPermission(options = {}) {
  try {
    await ensureLocationPermission(options)
    return await getLocation()
  } catch (error) {
    if (error?.code && error.handled) {
      throw error
    }

    const permissionState = await getLocationSettingState()
    if (!looksLikePermissionError(error) && permissionState !== false) {
      throw createLocationError(LOCATION_ERROR_CODES.LOCATION_UNAVAILABLE, error)
    }

    const code = await handlePermissionFlow(options)
    throw createLocationError(code, error, { handled: true })
  }
}
