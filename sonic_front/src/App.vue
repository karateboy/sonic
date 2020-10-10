<template>
  <div id="app">
    <b-navbar variant="info">
      <b-navbar-brand>MIST</b-navbar-brand>
      <b-navbar-toggle target="nav-collapse"></b-navbar-toggle>

      <b-collapse id="nav-collapse" is-nav>
        <b-navbar-nav>
          <b-nav-item to="/dashboard">Dashboard</b-nav-item>
          <b-nav-item to="/playback">Playback</b-nav-item>
        </b-navbar-nav>

        <!-- Right aligned nav items -->
        <b-navbar-nav class="ml-auto">
          <b-nav-item-dropdown text="Configuration" right>
            <b-dropdown-item @click="updateSetting" to="/setting"
              >Setting</b-dropdown-item
            >
            <b-dropdown-item @click="updateBackground" to="/background"
              >Background</b-dropdown-item
            >
            <b-dropdown-item href="#">Alarm</b-dropdown-item>
          </b-nav-item-dropdown>
        </b-navbar-nav>
      </b-collapse>
    </b-navbar>
    <b-modal
      id="modal"
      :title="modalTitle"
      header-bg-variant="info"
      hide-footer
    >
      <router-view name="inner" @hideModal="hideModal" />
    </b-modal>
    <router-view />
  </div>
</template>
<style scoped></style>
<script lang="ts">
import Vue from "vue";
export default Vue.extend({
  data() {
    return {
      modalTitle: "",
    };
  },
  methods: {
    updateSetting() {
      this.modalTitle = "Setting";
      this.$bvModal.show("modal");
    },
    updateBackground() {
      this.modalTitle = "Background";
      this.$bvModal.show("modal");
    },
    hideModal() {
      this.$bvModal.hide("modal");
    },
  },
});
</script>