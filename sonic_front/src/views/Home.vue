<template>
  <div>
    <b-card
      header="Status"
      header-bg-variant="primary"
      header-tag="h3"
      header-text-variant="white"
      border-variant="primary"
    >
      ...
      <br />
    </b-card>
    <br />
    <b-card
      header="Sound Level"
      header-bg-variant="primary"
      header-tag="h4"
      header-text-variant="white"
      border-variant="primary"
    >
      <b-table-simple striped>
        <b-tbody>
          <b-tr>
            <b-th>Max SEL:</b-th>
            <b-td>0</b-td>
            <b-th>SEL:</b-th>
            <b-td>0</b-td>
            <b-th>Max Lpeak:</b-th>
            <b-td>0</b-td>
          </b-tr>
          <b-tr>
            <b-th>SEL<small>5</small></b-th>
            <b-td>0</b-td>
            <b-th>SEL<small>10</small></b-th>
            <b-td>0</b-td>
            <b-th>SEL<small>50</small></b-th>
            <b-td>0</b-td>
          </b-tr>
          <b-tr>
            <b-th>SEL<small>90</small></b-th>
            <b-td>0</b-td>
            <b-th>SEL<small>95</small></b-th>
            <b-td>0</b-td>
            <b-th></b-th>
            <b-td></b-td>
          </b-tr>
        </b-tbody>
      </b-table-simple>
      <div id="soundChart"></div>
      <div id="octaveBandsChart"></div>
    </b-card>
    <br />
    <b-card
      header="Sound Level (30 sec)"
      header-bg-variant="primary"
      header-tag="h4"
      header-text-variant="white"
      border-variant="primary"
    >
      <b-table-simple striped>
        <b-tbody>
          <b-tr>
            <b-th>Max SEL:</b-th>
            <b-td>0</b-td>
            <b-th>SEL:</b-th>
            <b-td>0</b-td>
            <b-th>Max Lpeak:</b-th>
            <b-td>0</b-td>
          </b-tr>
          <b-tr>
            <b-th>SEL<small>5</small></b-th>
            <b-td>0</b-td>
            <b-th>SEL<small>10</small></b-th>
            <b-td>0</b-td>
            <b-th>SEL<small>50</small></b-th>
            <b-td>0</b-td>
          </b-tr>
          <b-tr>
            <b-th>SEL<small>90</small></b-th>
            <b-td>0</b-td>
            <b-th>SEL<small>95</small></b-th>
            <b-td>0</b-td>
            <b-th></b-th>
            <b-td></b-td>
          </b-tr>
        </b-tbody>
      </b-table-simple>
      <div id="soundChart30"></div>
      <div id="octaveBandsChart30"></div>
    </b-card>
  </div>
</template>

<script lang="ts">
import Vue from "vue";
import Highcharts from "highcharts";

let octFreq = [
  "6.3",
  "8",
  "10",
  "12.5",
  "16",
  "20",
  "25",
  "31.5",
  "40",
  "50",
  "63",
  "80",
  "100",
  "125",
  "160",
  "200",
  "250",
  "315",
  "400",
  "500",
  "630",
  "800",
  "1k",
  "1.25k",
  "1.6k",
  "2k",
  "2.5k",
  "3.15k",
  "4k",
  "5k",
  "6.3k",
  "8k",
  "10k",
  "12.5k",
  "16k",
  "20k",
];

import axios from "axios";

export default Vue.extend({
  mounted() {
    this.getRealtimeValues();
    this.drawSoundChart();
    this.drawSoundChart30();
    this.drawOctalChart();
    this.drawOctalChart30();
  },
  data(){
    return {
      connected: false,
      streaming: false,
      logging: false,
      backgroundLevel: 0
    }
  },
  methods: {
    getRealtimeValues() {
      axios
        .get("/realTimeValues")
        .then((res) => {
          if (res.status === 200) {
            //const ret = res.data;
            
          }
        })
        .catch((err) => {
          throw err;
        });
    },
    drawSoundChart() {
      let series: Array<Highcharts.SeriesSplineOptions> = [
        {
          type: "spline",
          name: "SEL,1s",
          data: [],
          tooltip: {
            pointFormat:
              '<tr><td style="color:{series.color};padding:0">{series.name}: </td>' +
              '<td style="padding:0"><b>{point.y:.1f} dB</b></td></tr>',
          },
        },
        {
          type: "spline",
          name: "Lpeak,1s",
          data: [],
          tooltip: {
            pointFormat:
              '<tr><td style="color:{series.color};padding:0">{series.name}: </td>' +
              '<td style="padding:0"><b>{point.y:.1f} dB</b></td></tr>',
          },
        },
      ];

      Highcharts.chart("soundChart", {
        chart: {
          type: "column",
        },
        title: {
          text: "SOUND LEVEL",
          align: "center",
          style: { fontSize: "24px" },
        },
        yAxis: {
          title: {
            text: "Sound Level (dB re 1uPa)",
          },
          labels: {
            format: "{value} dB",
          },
          max: 600,
        },
        xAxis: {
          type: "datetime",
        },
        credits: {
          enabled: false,
        },

        legend: {
          enabled: false,
        },
        tooltip: {
          headerFormat:
            '<span style="font-size:10px">{point.key}</span><table>',
          footerFormat: "</table>",
          shared: true,
          useHTML: true,
        },
        series,
      });
    },
    drawSoundChart30() {
      let series: Array<Highcharts.SeriesColumnOptions> = [
        {
          type: "column",
          name: "SEL,30s",
          yAxis: 0,
          data: [],
          tooltip: {
            pointFormat:
              '<tr><td style="color:{series.color};padding:0">{series.name}: </td>' +
              '<td style="padding:0"><b>{point.y:.1f} dB</b></td></tr>',
          },
        },
        {
          type: "column",
          name: "Epoch Counts,30s",
          yAxis: 1,
          data: [],
          tooltip: {
            pointFormat:
              '<tr><td style="color:{series.color};padding:0">{series.name}: </td>' +
              '<td style="padding:0"><b>{point.y} counts/30s</b></td></tr>',
          },
        },
        {
          type: "column",
          name: "LE(30s)",
          yAxis: 0,
          data: [],
          tooltip: {
            pointFormat:
              '<tr><td style="color:{series.color};padding:0">{series.name}: </td>' +
              '<td style="padding:0"><b>{point.y:.1f} dB</b></td></tr>',
          },
        },
        {
          type: "column",
          name: "SEL5%(30s)",
          yAxis: 0,
          data: [],
          tooltip: {
            pointFormat:
              '<tr><td style="color:{series.color};padding:0">{series.name}: </td>' +
              '<td style="padding:0"><b>{point.y:.1f} dB</b></td></tr>',
          },
        },
        {
          type: "column",
          name: "Leq,30s",
          yAxis: 0,
          data: [],
          tooltip: {
            pointFormat:
              '<tr><td style="color:{series.color};padding:0">{series.name}: </td>' +
              '<td style="padding:0"><b>{point.y:.1f} dB</b></td></tr>',
          },
        },
      ];

      Highcharts.chart("soundChart30", {
        chart: {
          type: "column",
          style: {
            fontFamily: "Microsoft JhengHei",
            fontSize: "14px",
            color: "#192E5B",
          },
        },
        title: {
          text: "SOUND LEVEL (30s)",
          align: "center",
          style: { fontSize: "24px" },
        },
        credits: {
          enabled: false,
        },

        xAxis: [
          {
            type: "datetime",
          },
        ],
        yAxis: [
          {
            title: {
              text: "Sound Level (dB re 1uPa)",
            },
            labels: {
              format: "{value} dB",
            },
          },
          {
            title: {
              text: "Counts (per 30s)",
            },
            labels: {
              format: "{value} counts/30s",
            },
            opposite: true,
            allowDecimals: false,
          },
        ],
        legend: {
          align: "left",
          verticalAlign: "top",
          borderWidth: 0,
        },
        tooltip: {
          headerFormat:
            '<span style="font-size:10px">{point.key}</span><table>',
          footerFormat: "</table>",
          shared: true,
          useHTML: true,
        },
        series,
      });
    },
    drawOctalChart() {
      var octaveBandsChartOptions: Highcharts.Options = {
        chart: {
          type: "column",
          zoomType: "y",
        },

        title: {
          text: "1 / 3 Octave Band",
          align: "center",
          style: { fontSize: "24px" },
        },
        xAxis: {
          categories: octFreq.slice(5, 36),
          crosshair: true,
        },
        yAxis: {
          min: 0,
          title: {
            text: "Sound Level (dB re 1uPa)",
          },
        },

        tooltip: {
          headerFormat:
            '<span style="font-size:10px">{point.key} Hz</span><table>',
          pointFormat:
            '<tr><td style="color:{series.color};padding:0">{series.name}: </td>' +
            '<td style="padding:0"><b>{point.y:.1f} dB</b></td></tr>',
          footerFormat: "</table>",
          shared: true,
          useHTML: true,
        },
        credits: {
          enabled: false,
        },

        series: [
          {
            type: "column",
            name: "1/3 Octave Bands",
            data: [
              1,
              2,
              3,
              4,
              5,
              6,
              7,
              8,
              9,
              10,
              1,
              2,
              3,
              4,
              5,
              6,
              7,
              8,
              9,
              10,
              1,
              2,
              3,
              4,
              5,
              6,
              7,
              8,
              9,
              10,
              1,
            ],
          },
        ],
      };
      Highcharts.chart("octaveBandsChart", octaveBandsChartOptions);
    },

    drawOctalChart30() {
      var octaveBandsChartOptions: Highcharts.Options = {
        chart: {
          type: "column",
          zoomType: "y",
        },

        title: {
          text: "1 / 3 Octave Band",
          align: "center",
          style: { fontSize: "24px" },
        },
        xAxis: {
          categories: octFreq.slice(5, 36),
          crosshair: true,
        },
        yAxis: {
          min: 0,
          title: {
            text: "Sound Level (dB re 1uPa)",
          },
        },

        tooltip: {
          headerFormat:
            '<span style="font-size:10px">{point.key} Hz</span><table>',
          pointFormat:
            '<tr><td style="color:{series.color};padding:0">{series.name}: </td>' +
            '<td style="padding:0"><b>{point.y:.1f} dB</b></td></tr>',
          footerFormat: "</table>",
          shared: true,
          useHTML: true,
        },
        credits: {
          enabled: false,
        },

        series: [
          {
            type: "column",
            name: "1/3 Octave Bands",
            data: [
              1,
              2,
              3,
              4,
              5,
              6,
              7,
              8,
              9,
              10,
              1,
              2,
              3,
              4,
              5,
              6,
              7,
              8,
              9,
              10,
              1,
              2,
              3,
              4,
              5,
              6,
              7,
              8,
              9,
              10,
              1,
            ],
          },
        ],
      };
      Highcharts.chart("octaveBandsChart30", octaveBandsChartOptions);
    },
  },
});
</script>