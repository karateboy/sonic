<template>
  <b-form>
    <b-form-group
      label="Background Level (db):"
      label-for="input-1"
      description="background."
    >
      <b-form-input
        id="input-1"
        v-model="form.background"
        type="number"
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
  data() {
    return {
      form: {
        background: 0,
      },
    };
  },
});
</script>