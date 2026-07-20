#!/usr/bin/env node
import assert from 'node:assert/strict';
import fs from 'node:fs';

const html=fs.readFileSync(new URL('../izleti-cloud.html',import.meta.url),'utf8');

assert.match(html,/value="upcoming" selected>Nadolazeći ovaj mjesec<\/option>/,'Upcoming filter is not the default.');
assert.match(html,/status==='upcoming'/,'Upcoming filter logic is missing.');
assert.match(html,/\['planned','active'\]\.includes\(t\.status\)/,'Upcoming filter does not limit statuses.');
assert.match(html,/isoDate\(t\.end_date\|\|t\.start_date\)>=today/,'Past trips are not excluded from upcoming.');
assert.match(html,/loadTrips\(\{force:true\}\)\.finally\(startTripsAutoSync\)/,'Initial page load is not forced fresh.');
assert.match(html,/setInterval\(refreshTripsInBackground,60000\)/,'Background refresh timer is missing.');
assert.match(html,/postgres_changes[^\n]+table:'sov_trips'/,'Realtime Trips refresh is missing.');
assert.match(html,/Nema nadolazećih izleta u ovom mjesecu\./,'Empty state is not specific to upcoming month.');

console.log('SOV TRIPS DEFAULT UPCOMING TEST PASSED');
