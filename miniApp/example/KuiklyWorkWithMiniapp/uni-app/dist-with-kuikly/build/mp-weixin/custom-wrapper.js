global.customWrapperCache = new Map()

function isString (o) {
  return typeof o === 'string'
}

Component({
  properties: {
    i: {
      type: Object,
      value: {
        nn: 7
      }
    }
  },
  options: {
    addGlobalClass: true,
    virtualHost: false
  },
  methods: {
    eh: function(event) {
      global.eventHandler(event)
    }
  },
  attached() {
    const componentId = this.data.i.sid
    if (isString(componentId)) {
      global.customWrapperCache.set(componentId, this)
    }
  },
  detached() {
    const componentId = this.data.i.sid
    if (isString(componentId)) {
      global.customWrapperCache.delete(componentId)
    }
  }
})