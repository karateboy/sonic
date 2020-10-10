<template>
  <b-form>
    <b-form-group
      label="Server listen port:"
      label-for="input-1"
      description="web server port."
    >
      <b-form-input
        id="input-1"
        v-model.number="form.serverPort"
        type="number"
        required
        placeholder="7000"
      ></b-form-input>
    </b-form-group>

    <b-form-group label="Wave Streaming URL:" label-for="input-2">
      <b-form-input
        id="input-2"
        v-model="form.url"
        required
        placeholder="60.253.23.25"
      ></b-form-input>
    </b-form-group>

    <b-form-group label="Wave Streaming Port:" label-for="input-3">
      <b-form-input
        id="input-3"
        v-model.number="form.wavePort"
        required
        type="number"
        placeholder="51678"
      ></b-form-input>
    </b-form-group>
    <b-form-group label="Epoch Streaming Port:" label-for="input-4">
      <b-form-input
        id="input-4"
        v-model.number="form.epochPort"
        required
        type="number"
        placeholder="51680"
      ></b-form-input>
    </b-form-group>
    <b-form-group label="Time Zone:" label-for="input-5">
      <b-form-input
        id="input-5"
        v-model.number="form.timeZone"
        required
        type="number"
        placeholder="8"
      ></b-form-input>
    </b-form-group>
    <b-form-group label="Logging Length:" label-for="input-6">
      <b-form-input
        id="input-6"
        v-model.number="form.recordLen"
        required
        type="number"
        placeholder="1"
      ></b-form-input>
    </b-form-group>
    <b-form-group label="Storage Path:" label-for="input-7">
      <b-form-input
        id="input-7"
        v-model="form.recordPath"
        required
        placeholder="/home/pi"
      ></b-form-input>
    </b-form-group>
    <b-form-group label="Sound Value Modify (dB):" label-for="input-8">
      <b-form-input
        id="input-8"
        v-model.number="form.modify"
        required
        placeholder="0"
      ></b-form-input>
    </b-form-group>
    <b-button variant="primary" @click.prevent="saveSetting">Submit</b-button>
    <b-button type="reset" variant="danger">Reset</b-button>
  </b-form>
</template>
<script lang="ts">
import Vue from "vue";
import axios from "axios";
export default Vue.extend({
  mounted() {
    this.getSetting();
  },
  data() {
    return {
      form: {
        serverPort: 0,
        url: "",
        wavePort: 123,
        epochPort: 1234,
        recordLen: 1,
        timeZone: 8,
        recordPath: "",
        modify: 0,
      },
    };
  },
  methods: {
    getSetting() {
      axios
        .get("/setting")
        .then((res) => {
          const ret = res.data;
          this.form.serverPort = ret.serverPort;
          this.form.url = ret.url;
          this.form.wavePort = ret.wavePort;
          this.form.epochPort = ret.epochPort;
          this.form.recordLen = ret.recordLen;
          this.form.recordPath = ret.recordPath;
          this.form.modify = ret.modify;
        })
        .catch((err) => {
          alert(err);
        });
    },
    saveSetting() {
      axios
        .post("/setting", this.form)
        .then((res) => {
          const ret = res.data;
          if (ret.ok)
            this.$bvModal.msgBoxOk("Success!").then(() => {
              this.$emit("hideModal");
            });
        })
        .catch((err) => alert(err));
    },
  },
});
</script>
