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
    virtualHost: true
  },
  methods: {
    eh: function(event) {
      global.eventHandler(event)
    }
  },
})