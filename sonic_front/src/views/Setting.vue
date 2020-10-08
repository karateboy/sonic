<template>
  <b-form>
    <b-form-group
      label="Server listen port:"
      label-for="input-1"
      description="web server port."
    >
      <b-form-input
        id="input-1"
        v-model="form.port"
        type="number"
        required
        placeholder="7000"
      ></b-form-input>
    </b-form-group>

    <b-form-group
      label="Wave Streaming URL:"
      label-for="input-2"
    >
      <b-form-input
        id="input-2"
        v-model="form.streamingURL"
        required
        placeholder="60.253.23.25"
      ></b-form-input>
    </b-form-group>

    <b-form-group
      label="Wave Streaming Port:"
      label-for="input-3"
    >
      <b-form-input
        id="input-3"
        v-model="form.waveStreamingPort"
        required
        type="number"
        placeholder="51678"
      ></b-form-input>
    </b-form-group>
    <b-form-group
      label="Epoch Streaming Port:"
      label-for="input-4"
    >
      <b-form-input
        id="input-4"
        v-model="form.epochStreamingPort"
        required
        type="number"
        placeholder="51680"
      ></b-form-input>
    </b-form-group>
    <b-form-group label="Time Zone:" label-for="input-5">
      <b-form-input
        id="input-5"
        v-model="form.timeZone"
        required
        type="number"
        placeholder="8"
      ></b-form-input>
    </b-form-group>
    <b-form-group label="Logging Length:" label-for="input-6">
      <b-form-input
        id="input-6"
        v-model="form.loggingLength"
        required
        type="number"
        placeholder="1"
      ></b-form-input>
    </b-form-group>
    <b-form-group label="Storage Path:" label-for="input-7">
      <b-form-input
        id="input-7"
        v-model="form.storagePath"
        required        
        placeholder="/home/pi"
      ></b-form-input>
    </b-form-group>
    <b-form-group label="Sound Value Modify (dB):" label-for="input-8">
      <b-form-input
        id="input-8"
        v-model="form.soundModify"
        required        
        placeholder="0"
      ></b-form-input>
    </b-form-group>
    <b-button type="submit" variant="primary">Submit</b-button>
    <b-button type="reset" variant="danger">Reset</b-button>
  </b-form>
</template>
<script lang="ts">
import Vue from "vue";
export default Vue.extend({
  data(){
    return {
      form: {
        port:0,
        streamingURL:"60.253.23.25",
        waveStreamingPort: 1234,
        loggingLength: 1,
        epochStreamingPort: 1,
        timeZone:8,
        storagePath: "",
        soundModify:""

      }
    }
  },
  methods: {},
});
</script>
