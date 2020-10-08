import Vue from 'vue'
import VueRouter, { RouteConfig } from 'vue-router'
import Home from '../views/Home.vue'
import Setting from "../views/Setting.vue"
Vue.use(VueRouter)

const routes: Array<RouteConfig> = [
  {
    path: '/dashboard',
    name: 'dashboard',
    component: Home,    
  },
  {
    path: '/setting',
    name: 'Setting',
    components:{
      default: Home,
      "inner": Setting
    }
  },
  {
    path: '/background',
    name: 'Background',
    components:{
      default: Home,
      "inner": () => import(/* webpackChunkName: "about" */ '../views/Background.vue')
    }
  },
  {
    path: '/playback',
    name: 'Playback',
    component: () => import(/* webpackChunkName: "about" */ '../views/Playback.vue')
  },
  {
    path: "/",
    redirect: "/dashboard"
  }
]

const router = new VueRouter({
  mode: 'history',
  base: process.env.BASE_URL,
  routes
})

export default router
