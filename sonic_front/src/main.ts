import Vue from 'vue'
import App from './App.vue'
import router from './router'
import store from './store'
import 'bootstrap/dist/css/bootstrap.css'
import 'bootstrap-vue/dist/bootstrap-vue.css'
import { BootstrapVue, IconsPlugin} from "bootstrap-vue"
import Highcharts from "highcharts"
import ex from "highcharts/modules/exporting";
import csv from "highcharts/modules/export-data"
import offline_export from "highcharts/modules/offline-exporting";

ex(Highcharts);
csv(Highcharts)
offline_export(Highcharts);

Vue.config.productionTip = false
Vue.use(BootstrapVue)
Vue.use(IconsPlugin)

new Vue({
  router,
  store,
  render: h => h(App)
}).$mount('#app')
