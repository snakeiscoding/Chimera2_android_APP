Web timezone to console timezone mapping.

- Rows marked **(closest)** have no console entry with the same clock time. The console uses the
  closest entry instead (at most 1 hour off) and logs a warning.
- **Legacy names** such as US/Central are not in this table. The console looks up the zone
  with the same time rules (US/Central is America/Chicago), uses that row and logs a warning.
- **Default: US Central (America/Chicago).** If the web sends a timezone that is not in
  this table and is not a legacy name for one, the console uses US Central and logs an error.

About **index 28 and index 30**:
- Both entries are backed by the same IANA timezone, Europe/Belgrade, so their clock time is the
  same. Only their labels differ: 28 is "Belgrade, Bratislava, Budapest, Ljubljana, Prague" and
  30 is "Sarajevo, Skopje, Warsaw, Zagreb".
- The mapping normally finds the console entry by IANA id, which would always give 28. For these
  cities it uses a fixed index instead, so each city gets the entry whose label names it:
  Sarajevo, Skopje, Warsaw and Zagreb go to 30; Belgrade, Bratislava, Budapest, Ljubljana and
  Prague go to 28. Podgorica and Tirane are in neither label and stay on 28.
- The fixed index is only used while that entry is still Europe/Belgrade. If the console list is
  reordered, the mapping uses the first Europe/Belgrade entry, which shows the same clock time.

| Web timezone | Index | Console timezone | Console display (English) |
|---|---|---|---|
| Africa/Abidjan | 25 | Africa/Monrovia | (GMT) Casablanca, Monrovia |
| Africa/Accra | 25 | Africa/Monrovia | (GMT) Casablanca, Monrovia |
| Africa/Addis_Ababa | 42 | Africa/Nairobi | (GMT+03:00) Nairobi |
| Africa/Algiers | 31 | Africa/Lagos | (GMT+01:00) West Central Africa |
| Africa/Asmera | 42 | Africa/Nairobi | (GMT+03:00) Nairobi |
| Africa/Bamako | 25 | Africa/Monrovia | (GMT) Casablanca, Monrovia |
| Africa/Bangui | 31 | Africa/Lagos | (GMT+01:00) West Central Africa |
| Africa/Banjul | 25 | Africa/Monrovia | (GMT) Casablanca, Monrovia |
| Africa/Bissau | 25 | Africa/Monrovia | (GMT) Casablanca, Monrovia |
| Africa/Blantyre | 34 | Africa/Harare | (GMT+02:00) Harare, Pretoria |
| Africa/Brazzaville | 31 | Africa/Lagos | (GMT+01:00) West Central Africa |
| Africa/Bujumbura | 34 | Africa/Harare | (GMT+02:00) Harare, Pretoria |
| Africa/Cairo | 33 | Africa/Cairo | (GMT+02:00) Cairo |
| Africa/Casablanca | 31 | Africa/Lagos | (GMT+01:00) West Central Africa |
| Africa/Ceuta | 29 | Europe/Paris | (GMT+01:00) Brussels, Copenhagen, Madrid, Paris |
| Africa/Conakry | 25 | Africa/Monrovia | (GMT) Casablanca, Monrovia |
| Africa/Dakar | 25 | Africa/Monrovia | (GMT) Casablanca, Monrovia |
| Africa/Dar_es_Salaam | 42 | Africa/Nairobi | (GMT+03:00) Nairobi |
| Africa/Djibouti | 42 | Africa/Nairobi | (GMT+03:00) Nairobi |
| Africa/Douala | 31 | Africa/Lagos | (GMT+01:00) West Central Africa |
| Africa/El_Aaiun | 31 | Africa/Lagos | (GMT+01:00) West Central Africa |
| Africa/Freetown | 25 | Africa/Monrovia | (GMT) Casablanca, Monrovia |
| Africa/Gaborone | 34 | Africa/Harare | (GMT+02:00) Harare, Pretoria |
| Africa/Harare | 34 | Africa/Harare | (GMT+02:00) Harare, Pretoria |
| Africa/Johannesburg | 34 | Africa/Harare | (GMT+02:00) Harare, Pretoria |
| Africa/Juba | 34 | Africa/Harare | (GMT+02:00) Harare, Pretoria |
| Africa/Kampala | 42 | Africa/Nairobi | (GMT+03:00) Nairobi |
| Africa/Khartoum | 34 | Africa/Harare | (GMT+02:00) Harare, Pretoria |
| Africa/Kigali | 34 | Africa/Harare | (GMT+02:00) Harare, Pretoria |
| Africa/Kinshasa | 31 | Africa/Lagos | (GMT+01:00) West Central Africa |
| Africa/Lagos | 31 | Africa/Lagos | (GMT+01:00) West Central Africa |
| Africa/Libreville | 31 | Africa/Lagos | (GMT+01:00) West Central Africa |
| Africa/Lome | 25 | Africa/Monrovia | (GMT) Casablanca, Monrovia |
| Africa/Luanda | 31 | Africa/Lagos | (GMT+01:00) West Central Africa |
| Africa/Lubumbashi | 34 | Africa/Harare | (GMT+02:00) Harare, Pretoria |
| Africa/Lusaka | 34 | Africa/Harare | (GMT+02:00) Harare, Pretoria |
| Africa/Malabo | 31 | Africa/Lagos | (GMT+01:00) West Central Africa |
| Africa/Maputo | 34 | Africa/Harare | (GMT+02:00) Harare, Pretoria |
| Africa/Maseru | 34 | Africa/Harare | (GMT+02:00) Harare, Pretoria |
| Africa/Mbabane | 34 | Africa/Harare | (GMT+02:00) Harare, Pretoria |
| Africa/Mogadishu | 42 | Africa/Nairobi | (GMT+03:00) Nairobi |
| Africa/Monrovia | 25 | Africa/Monrovia | (GMT) Casablanca, Monrovia |
| Africa/Nairobi | 42 | Africa/Nairobi | (GMT+03:00) Nairobi |
| Africa/Ndjamena | 31 | Africa/Lagos | (GMT+01:00) West Central Africa |
| Africa/Niamey | 31 | Africa/Lagos | (GMT+01:00) West Central Africa |
| Africa/Nouakchott | 25 | Africa/Monrovia | (GMT) Casablanca, Monrovia |
| Africa/Ouagadougou | 25 | Africa/Monrovia | (GMT) Casablanca, Monrovia |
| Africa/Porto-Novo | 31 | Africa/Lagos | (GMT+01:00) West Central Africa |
| Africa/Sao_Tome | 25 | Africa/Monrovia | (GMT) Casablanca, Monrovia |
| Africa/Tripoli | 34 | Africa/Harare | (GMT+02:00) Harare, Pretoria |
| Africa/Tunis | 31 | Africa/Lagos | (GMT+01:00) West Central Africa |
| Africa/Windhoek | 38 | Africa/Windhoek | (GMT+02:00) Windhoek |
| America/Adak | 1 | Pacific/Honolulu (closest) | (GMT-10:00) Hawaii |
| America/Anchorage | 2 | America/Anchorage | (GMT-09:00) Alaska |
| America/Anguilla | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Antigua | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Araguaina | 21 | America/Sao_Paulo | (GMT-03:00) Brasilia |
| America/Argentina/La_Rioja | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| America/Argentina/Rio_Gallegos | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| America/Argentina/Salta | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| America/Argentina/San_Juan | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| America/Argentina/San_Luis | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| America/Argentina/Tucuman | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| America/Argentina/Ushuaia | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| America/Aruba | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Asuncion | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| America/Bahia | 21 | America/Sao_Paulo | (GMT-03:00) Brasilia |
| America/Bahia_Banderas | 8 | America/Monterrey | (GMT-06:00) Guadalajara, Mexico City, Monterrey |
| America/Barbados | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Belem | 21 | America/Sao_Paulo | (GMT-03:00) Brasilia |
| America/Belize | 10 | America/Costa_Rica | (GMT-06:00) Central America |
| America/Blanc-Sablon | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Boa_Vista | 15 | America/Cuiaba | (GMT-04:00) Manaus |
| America/Bogota | 13 | America/Bogota | (GMT-05:00) Bogota, Lima, Quito |
| America/Boise | 4 | America/Denver | (GMT-07:00) Mountain Time (US and Canada) |
| America/Buenos_Aires | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| America/Cambridge_Bay | 4 | America/Denver | (GMT-07:00) Mountain Time (US and Canada) |
| America/Campo_Grande | 15 | America/Cuiaba | (GMT-04:00) Manaus |
| America/Cancun | 13 | America/Bogota | (GMT-05:00) Bogota, Lima, Quito |
| America/Caracas | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Catamarca | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| America/Cayenne | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| America/Cayman | 13 | America/Bogota | (GMT-05:00) Bogota, Lima, Quito |
| America/Chicago | 9 | America/Chicago | (GMT-06:00) Central Time (US and Canada) |
| America/Chihuahua | 5 | America/Chihuahua | (GMT-07:00) Chihuahua, La Paz, Mazatlan |
| America/Ciudad_Juarez | 4 | America/Denver | (GMT-07:00) Mountain Time (US and Canada) |
| America/Coral_Harbour | 13 | America/Bogota | (GMT-05:00) Bogota, Lima, Quito |
| America/Cordoba | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| America/Costa_Rica | 10 | America/Costa_Rica | (GMT-06:00) Central America |
| America/Coyhaique | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| America/Creston | 6 | America/Phoenix | (GMT-07:00) Arizona |
| America/Cuiaba | 15 | America/Cuiaba | (GMT-04:00) Manaus |
| America/Curacao | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Danmarkshavn | 25 | Africa/Monrovia | (GMT) Casablanca, Monrovia |
| America/Dawson | 6 | America/Phoenix | (GMT-07:00) Arizona |
| America/Dawson_Creek | 6 | America/Phoenix | (GMT-07:00) Arizona |
| America/Denver | 4 | America/Denver | (GMT-07:00) Mountain Time (US and Canada) |
| America/Detroit | 12 | America/New_York | (GMT-05:00) Eastern Time (US and Canada) |
| America/Dominica | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Edmonton | 4 | America/Denver | (GMT-07:00) Mountain Time (US and Canada) |
| America/Eirunepe | 13 | America/Bogota | (GMT-05:00) Bogota, Lima, Quito |
| America/El_Salvador | 10 | America/Costa_Rica | (GMT-06:00) Central America |
| America/Fort_Nelson | 6 | America/Phoenix | (GMT-07:00) Arizona |
| America/Fortaleza | 21 | America/Sao_Paulo | (GMT-03:00) Brasilia |
| America/Glace_Bay | 17 | America/Halifax | (GMT-04:00) Atlantic Time (Canada) |
| America/Godthab | 19 | America/Godthab | (GMT-03:00) Greenland |
| America/Goose_Bay | 17 | America/Halifax | (GMT-04:00) Atlantic Time (Canada) |
| America/Grand_Turk | 12 | America/New_York | (GMT-05:00) Eastern Time (US and Canada) |
| America/Grenada | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Guadeloupe | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Guatemala | 10 | America/Costa_Rica | (GMT-06:00) Central America |
| America/Guayaquil | 13 | America/Bogota | (GMT-05:00) Bogota, Lima, Quito |
| America/Guyana | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Halifax | 17 | America/Halifax | (GMT-04:00) Atlantic Time (Canada) |
| America/Havana | 12 | America/New_York | (GMT-05:00) Eastern Time (US and Canada) |
| America/Hermosillo | 6 | America/Phoenix | (GMT-07:00) Arizona |
| America/Indiana/Knox | 9 | America/Chicago | (GMT-06:00) Central Time (US and Canada) |
| America/Indiana/Marengo | 11 | America/Indiana/Indianapolis | (GMT-05:00) Indiana (East) |
| America/Indiana/Petersburg | 11 | America/Indiana/Indianapolis | (GMT-05:00) Indiana (East) |
| America/Indiana/Tell_City | 9 | America/Chicago | (GMT-06:00) Central Time (US and Canada) |
| America/Indiana/Vevay | 11 | America/Indiana/Indianapolis | (GMT-05:00) Indiana (East) |
| America/Indiana/Vincennes | 11 | America/Indiana/Indianapolis | (GMT-05:00) Indiana (East) |
| America/Indiana/Winamac | 11 | America/Indiana/Indianapolis | (GMT-05:00) Indiana (East) |
| America/Indianapolis | 11 | America/Indiana/Indianapolis | (GMT-05:00) Indiana (East) |
| America/Inuvik | 4 | America/Denver | (GMT-07:00) Mountain Time (US and Canada) |
| America/Iqaluit | 12 | America/New_York | (GMT-05:00) Eastern Time (US and Canada) |
| America/Jamaica | 13 | America/Bogota | (GMT-05:00) Bogota, Lima, Quito |
| America/Jujuy | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| America/Juneau | 2 | America/Anchorage | (GMT-09:00) Alaska |
| America/Kentucky/Monticello | 12 | America/New_York | (GMT-05:00) Eastern Time (US and Canada) |
| America/Kralendijk | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/La_Paz | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Lima | 13 | America/Bogota | (GMT-05:00) Bogota, Lima, Quito |
| America/Los_Angeles | 3 | America/Tijuana | (GMT-08:00) Pacific Time (US and Canada); Tijuana |
| America/Louisville | 12 | America/New_York | (GMT-05:00) Eastern Time (US and Canada) |
| America/Lower_Princes | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Maceio | 21 | America/Sao_Paulo | (GMT-03:00) Brasilia |
| America/Managua | 10 | America/Costa_Rica | (GMT-06:00) Central America |
| America/Manaus | 15 | America/Cuiaba | (GMT-04:00) Manaus |
| America/Marigot | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Martinique | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Matamoros | 9 | America/Chicago | (GMT-06:00) Central Time (US and Canada) |
| America/Mazatlan | 6 | America/Phoenix | (GMT-07:00) Arizona |
| America/Mendoza | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| America/Menominee | 9 | America/Chicago | (GMT-06:00) Central Time (US and Canada) |
| America/Merida | 8 | America/Monterrey | (GMT-06:00) Guadalajara, Mexico City, Monterrey |
| America/Metlakatla | 2 | America/Anchorage | (GMT-09:00) Alaska |
| America/Mexico_City | 8 | America/Monterrey | (GMT-06:00) Guadalajara, Mexico City, Monterrey |
| America/Miquelon | 18 | America/St_Johns (closest) | (GMT-03:30) Newfoundland and Labrador |
| America/Moncton | 17 | America/Halifax | (GMT-04:00) Atlantic Time (Canada) |
| America/Monterrey | 8 | America/Monterrey | (GMT-06:00) Guadalajara, Mexico City, Monterrey |
| America/Montevideo | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| America/Montserrat | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Nassau | 12 | America/New_York | (GMT-05:00) Eastern Time (US and Canada) |
| America/New_York | 12 | America/New_York | (GMT-05:00) Eastern Time (US and Canada) |
| America/Nome | 2 | America/Anchorage | (GMT-09:00) Alaska |
| America/Noronha | 22 | Atlantic/South_Georgia | (GMT-02:00) Mid-Atlantic |
| America/North_Dakota/Beulah | 9 | America/Chicago | (GMT-06:00) Central Time (US and Canada) |
| America/North_Dakota/Center | 9 | America/Chicago | (GMT-06:00) Central Time (US and Canada) |
| America/North_Dakota/New_Salem | 9 | America/Chicago | (GMT-06:00) Central Time (US and Canada) |
| America/Ojinaga | 9 | America/Chicago | (GMT-06:00) Central Time (US and Canada) |
| America/Panama | 13 | America/Bogota | (GMT-05:00) Bogota, Lima, Quito |
| America/Paramaribo | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| America/Phoenix | 6 | America/Phoenix | (GMT-07:00) Arizona |
| America/Port_of_Spain | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Port-au-Prince | 12 | America/New_York | (GMT-05:00) Eastern Time (US and Canada) |
| America/Porto_Velho | 15 | America/Cuiaba | (GMT-04:00) Manaus |
| America/Puerto_Rico | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Punta_Arenas | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| America/Rankin_Inlet | 9 | America/Chicago | (GMT-06:00) Central Time (US and Canada) |
| America/Recife | 21 | America/Sao_Paulo | (GMT-03:00) Brasilia |
| America/Regina | 7 | America/Regina | (GMT-06:00) Saskatchewan |
| America/Resolute | 9 | America/Chicago | (GMT-06:00) Central Time (US and Canada) |
| America/Rio_Branco | 13 | America/Bogota | (GMT-05:00) Bogota, Lima, Quito |
| America/Santarem | 21 | America/Sao_Paulo | (GMT-03:00) Brasilia |
| America/Santiago | 14 | America/Santiago | (GMT-04:00) Santiago |
| America/Santo_Domingo | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Sao_Paulo | 21 | America/Sao_Paulo | (GMT-03:00) Brasilia |
| America/Scoresbysund | 19 | America/Godthab | (GMT-03:00) Greenland |
| America/Sitka | 2 | America/Anchorage | (GMT-09:00) Alaska |
| America/St_Barthelemy | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/St_Johns | 18 | America/St_Johns | (GMT-03:30) Newfoundland and Labrador |
| America/St_Kitts | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/St_Lucia | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/St_Thomas | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/St_Vincent | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Swift_Current | 7 | America/Regina | (GMT-06:00) Saskatchewan |
| America/Tegucigalpa | 10 | America/Costa_Rica | (GMT-06:00) Central America |
| America/Thule | 17 | America/Halifax | (GMT-04:00) Atlantic Time (Canada) |
| America/Tijuana | 3 | America/Tijuana | (GMT-08:00) Pacific Time (US and Canada); Tijuana |
| America/Toronto | 12 | America/New_York | (GMT-05:00) Eastern Time (US and Canada) |
| America/Tortola | 16 | America/La_Paz | (GMT-04:00) Caracas, La Paz |
| America/Vancouver | 3 | America/Tijuana | (GMT-08:00) Pacific Time (US and Canada); Tijuana |
| America/Whitehorse | 6 | America/Phoenix | (GMT-07:00) Arizona |
| America/Winnipeg | 9 | America/Chicago | (GMT-06:00) Central Time (US and Canada) |
| America/Yakutat | 2 | America/Anchorage | (GMT-09:00) Alaska |
| Antarctica/Casey | 59 | Asia/Shanghai | (GMT+08:00) Beijing, Chongqing, Hong Kong SAR, Urumqi |
| Antarctica/Davis | 54 | Asia/Novosibirsk | (GMT+06:00) Almaty, Novosibirsk |
| Antarctica/DumontDUrville | 69 | Australia/Brisbane | (GMT+10:00) Brisbane |
| Antarctica/Macquarie | 70 | Australia/Sydney | (GMT+10:00) Canberra, Melbourne, Sydney |
| Antarctica/Mawson | 49 | Asia/Yekaterinburg | (GMT+05:00) Ekaterinburg |
| Antarctica/McMurdo | 75 | Pacific/Auckland | (GMT+12:00) Auckland, Wellington |
| Antarctica/Palmer | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| Antarctica/Rothera | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| Antarctica/Syowa | 40 | Asia/Kuwait | (GMT+03:00) Kuwait, Riyadh |
| Antarctica/Troll | 27 | Europe/Berlin (closest) | (GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna |
| Antarctica/Vostok | 49 | Asia/Yekaterinburg | (GMT+05:00) Ekaterinburg |
| Arctic/Longyearbyen | 27 | Europe/Berlin | (GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna |
| Asia/Aden | 40 | Asia/Kuwait | (GMT+03:00) Kuwait, Riyadh |
| Asia/Almaty | 55 | Asia/Almaty | (GMT+06:00) Astana, Dhaka |
| Asia/Amman | 39 | Asia/Baghdad | (GMT+03:00) Baghdad |
| Asia/Anadyr | 76 | Pacific/Fiji | (GMT+12:00) Fiji Islands, Kamchatka, Marshall Islands |
| Asia/Aqtau | 55 | Asia/Almaty | (GMT+06:00) Astana, Dhaka |
| Asia/Aqtobe | 55 | Asia/Almaty | (GMT+06:00) Astana, Dhaka |
| Asia/Ashgabat | 50 | Asia/Tashkent | (GMT+05:00) Islamabad, Karachi, Tashkent |
| Asia/Atyrau | 55 | Asia/Almaty | (GMT+06:00) Astana, Dhaka |
| Asia/Baghdad | 39 | Asia/Baghdad | (GMT+03:00) Baghdad |
| Asia/Bahrain | 39 | Asia/Baghdad | (GMT+03:00) Baghdad |
| Asia/Baku | 45 | Asia/Baku | (GMT+04:00) Baku |
| Asia/Bangkok | 57 | Asia/Bangkok | (GMT+07:00) Bangkok, Hanoi, Jakarta |
| Asia/Barnaul | 54 | Asia/Novosibirsk | (GMT+06:00) Almaty, Novosibirsk |
| Asia/Beirut | 36 | Asia/Jerusalem | (GMT+02:00) Jerusalem |
| Asia/Bishkek | 53 | Asia/Kathmandu (closest) | (GMT+05:45) Kathmandu |
| Asia/Brunei | 61 | Asia/Singapore | (GMT+08:00) Kuala Lumpur, Singapore |
| Asia/Calcutta | 51 | Asia/Calcutta | (GMT+05:30) Chennai, Kolkata, Mumbai, New Delhi |
| Asia/Chita | 66 | Asia/Yakutsk | (GMT+09:00) Yakutsk |
| Asia/Colombo | 52 | Asia/Colombo | (GMT+05:30) Sri Jayawardenepura |
| Asia/Damascus | 39 | Asia/Baghdad | (GMT+03:00) Baghdad |
| Asia/Dhaka | 53 | Asia/Kathmandu (closest) | (GMT+05:45) Kathmandu |
| Asia/Dili | 64 | Asia/Tokyo | (GMT+09:00) Osaka, Sapporo, Tokyo |
| Asia/Dubai | 44 | Asia/Muscat | (GMT+04:00) Abu Dhabi, Muscat |
| Asia/Dushanbe | 50 | Asia/Tashkent | (GMT+05:00) Islamabad, Karachi, Tashkent |
| Asia/Famagusta | 36 | Asia/Jerusalem | (GMT+02:00) Jerusalem |
| Asia/Gaza | 36 | Asia/Jerusalem | (GMT+02:00) Jerusalem |
| Asia/Hebron | 36 | Asia/Jerusalem | (GMT+02:00) Jerusalem |
| Asia/Hong_Kong | 59 | Asia/Shanghai | (GMT+08:00) Beijing, Chongqing, Hong Kong SAR, Urumqi |
| Asia/Hovd | 54 | Asia/Novosibirsk | (GMT+06:00) Almaty, Novosibirsk |
| Asia/Irkutsk | 60 | Asia/Irkutsk | (GMT+08:00) Irkutsk, Ulaanbaatar |
| Asia/Jakarta | 57 | Asia/Bangkok | (GMT+07:00) Bangkok, Hanoi, Jakarta |
| Asia/Jayapura | 64 | Asia/Tokyo | (GMT+09:00) Osaka, Sapporo, Tokyo |
| Asia/Jerusalem | 36 | Asia/Jerusalem | (GMT+02:00) Jerusalem |
| Asia/Kabul | 48 | Asia/Kabul | (GMT+04:30) Kabul |
| Asia/Kamchatka | 76 | Pacific/Fiji | (GMT+12:00) Fiji Islands, Kamchatka, Marshall Islands |
| Asia/Karachi | 50 | Asia/Tashkent | (GMT+05:00) Islamabad, Karachi, Tashkent |
| Asia/Katmandu | 53 | Asia/Kathmandu | (GMT+05:45) Kathmandu |
| Asia/Khandyga | 66 | Asia/Yakutsk | (GMT+09:00) Yakutsk |
| Asia/Krasnoyarsk | 58 | Asia/Krasnoyarsk | (GMT+07:00) Krasnoyarsk |
| Asia/Kuala_Lumpur | 61 | Asia/Singapore | (GMT+08:00) Kuala Lumpur, Singapore |
| Asia/Kuching | 61 | Asia/Singapore | (GMT+08:00) Kuala Lumpur, Singapore |
| Asia/Kuwait | 40 | Asia/Kuwait | (GMT+03:00) Kuwait, Riyadh |
| Asia/Macau | 59 | Asia/Shanghai | (GMT+08:00) Beijing, Chongqing, Hong Kong SAR, Urumqi |
| Asia/Magadan | 74 | Pacific/Guadalcanal | (GMT+11:00) Magadan, Solomon Islands, New Caledonia |
| Asia/Makassar | 61 | Asia/Singapore | (GMT+08:00) Kuala Lumpur, Singapore |
| Asia/Manila | 61 | Asia/Singapore | (GMT+08:00) Kuala Lumpur, Singapore |
| Asia/Muscat | 44 | Asia/Muscat | (GMT+04:00) Abu Dhabi, Muscat |
| Asia/Nicosia | 36 | Asia/Jerusalem | (GMT+02:00) Jerusalem |
| Asia/Novokuznetsk | 54 | Asia/Novosibirsk | (GMT+06:00) Almaty, Novosibirsk |
| Asia/Novosibirsk | 54 | Asia/Novosibirsk | (GMT+06:00) Almaty, Novosibirsk |
| Asia/Omsk | 53 | Asia/Kathmandu (closest) | (GMT+05:45) Kathmandu |
| Asia/Oral | 55 | Asia/Almaty | (GMT+06:00) Astana, Dhaka |
| Asia/Phnom_Penh | 57 | Asia/Bangkok | (GMT+07:00) Bangkok, Hanoi, Jakarta |
| Asia/Pontianak | 57 | Asia/Bangkok | (GMT+07:00) Bangkok, Hanoi, Jakarta |
| Asia/Pyongyang | 65 | Asia/Seoul | (GMT+09:00) Seoul |
| Asia/Qatar | 39 | Asia/Baghdad | (GMT+03:00) Baghdad |
| Asia/Qostanay | 55 | Asia/Almaty | (GMT+06:00) Astana, Dhaka |
| Asia/Qyzylorda | 55 | Asia/Almaty | (GMT+06:00) Astana, Dhaka |
| Asia/Rangoon | 56 | Asia/Rangoon | (GMT+06:30) Yangon (Rangoon) |
| Asia/Riyadh | 40 | Asia/Kuwait | (GMT+03:00) Kuwait, Riyadh |
| Asia/Saigon | 57 | Asia/Bangkok | (GMT+07:00) Bangkok, Hanoi, Jakarta |
| Asia/Sakhalin | 74 | Pacific/Guadalcanal | (GMT+11:00) Magadan, Solomon Islands, New Caledonia |
| Asia/Samarkand | 50 | Asia/Tashkent | (GMT+05:00) Islamabad, Karachi, Tashkent |
| Asia/Seoul | 65 | Asia/Seoul | (GMT+09:00) Seoul |
| Asia/Shanghai | 59 | Asia/Shanghai | (GMT+08:00) Beijing, Chongqing, Hong Kong SAR, Urumqi |
| Asia/Singapore | 61 | Asia/Singapore | (GMT+08:00) Kuala Lumpur, Singapore |
| Asia/Srednekolymsk | 74 | Pacific/Guadalcanal | (GMT+11:00) Magadan, Solomon Islands, New Caledonia |
| Asia/Taipei | 63 | Asia/Taipei | (GMT+08:00) Taipei |
| Asia/Tashkent | 50 | Asia/Tashkent | (GMT+05:00) Islamabad, Karachi, Tashkent |
| Asia/Tbilisi | 46 | Asia/Tbilisi | (GMT+04:00) Tblisi |
| Asia/Tehran | 43 | Asia/Tehran | (GMT+03:30) Tehran |
| Asia/Thimphu | 53 | Asia/Kathmandu (closest) | (GMT+05:45) Kathmandu |
| Asia/Tokyo | 64 | Asia/Tokyo | (GMT+09:00) Osaka, Sapporo, Tokyo |
| Asia/Tomsk | 54 | Asia/Novosibirsk | (GMT+06:00) Almaty, Novosibirsk |
| Asia/Ulaanbaatar | 60 | Asia/Irkutsk | (GMT+08:00) Irkutsk, Ulaanbaatar |
| Asia/Urumqi | 53 | Asia/Kathmandu (closest) | (GMT+05:45) Kathmandu |
| Asia/Ust-Nera | 73 | Asia/Vladivostok | (GMT+10:00) Vladivostok |
| Asia/Vientiane | 57 | Asia/Bangkok | (GMT+07:00) Bangkok, Hanoi, Jakarta |
| Asia/Vladivostok | 73 | Asia/Vladivostok | (GMT+10:00) Vladivostok |
| Asia/Yakutsk | 66 | Asia/Yakutsk | (GMT+09:00) Yakutsk |
| Asia/Yekaterinburg | 49 | Asia/Yekaterinburg | (GMT+05:00) Ekaterinburg |
| Asia/Yerevan | 47 | Asia/Yerevan | (GMT+04:00) Yerevan |
| Atlantic/Azores | 24 | Atlantic/Azores | (GMT-01:00) Azores |
| Atlantic/Bermuda | 17 | America/Halifax | (GMT-04:00) Atlantic Time (Canada) |
| Atlantic/Canary | 26 | Europe/London | (GMT) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London |
| Atlantic/Cape_Verde | 23 | Atlantic/Cape_Verde | (GMT-01:00) Cape Verde Islands |
| Atlantic/Faeroe | 26 | Europe/London | (GMT) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London |
| Atlantic/Madeira | 26 | Europe/London | (GMT) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London |
| Atlantic/Reykjavik | 25 | Africa/Monrovia | (GMT) Casablanca, Monrovia |
| Atlantic/South_Georgia | 22 | Atlantic/South_Georgia | (GMT-02:00) Mid-Atlantic |
| Atlantic/St_Helena | 25 | Africa/Monrovia | (GMT) Casablanca, Monrovia |
| Atlantic/Stanley | 20 | America/Argentina/Buenos_Aires | (GMT-03:00) Buenos Aires, Georgetown |
| Australia/Adelaide | 67 | Australia/Adelaide | (GMT+09:30) Adelaide |
| Australia/Brisbane | 69 | Australia/Brisbane | (GMT+10:00) Brisbane |
| Australia/Broken_Hill | 67 | Australia/Adelaide | (GMT+09:30) Adelaide |
| Australia/Darwin | 68 | Australia/Darwin | (GMT+09:30) Darwin |
| Australia/Eucla | 64 | Asia/Tokyo (closest) | (GMT+09:00) Osaka, Sapporo, Tokyo |
| Australia/Hobart | 72 | Australia/Hobart | (GMT+10:00) Hobart |
| Australia/Lindeman | 69 | Australia/Brisbane | (GMT+10:00) Brisbane |
| Australia/Lord_Howe | 70 | Australia/Sydney (closest) | (GMT+10:00) Canberra, Melbourne, Sydney |
| Australia/Melbourne | 70 | Australia/Sydney | (GMT+10:00) Canberra, Melbourne, Sydney |
| Australia/Perth | 62 | Australia/Perth | (GMT+08:00) Perth |
| Australia/Sydney | 70 | Australia/Sydney | (GMT+10:00) Canberra, Melbourne, Sydney |
| Europe/Amsterdam | 27 | Europe/Berlin | (GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna |
| Europe/Andorra | 29 | Europe/Paris | (GMT+01:00) Brussels, Copenhagen, Madrid, Paris |
| Europe/Astrakhan | 44 | Asia/Muscat | (GMT+04:00) Abu Dhabi, Muscat |
| Europe/Athens | 35 | Europe/Riga | (GMT+02:00) Helsinki, Kiev, Riga, Sofia, Tallinn, Vilnius |
| Europe/Belgrade | 28 | Europe/Belgrade | (GMT+01:00) Belgrade, Bratislava, Budapest, Ljubljana, Prague |
| Europe/Berlin | 27 | Europe/Berlin | (GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna |
| Europe/Bratislava | 28 | Europe/Belgrade | (GMT+01:00) Belgrade, Bratislava, Budapest, Ljubljana, Prague |
| Europe/Brussels | 29 | Europe/Paris | (GMT+01:00) Brussels, Copenhagen, Madrid, Paris |
| Europe/Bucharest | 35 | Europe/Riga | (GMT+02:00) Helsinki, Kiev, Riga, Sofia, Tallinn, Vilnius |
| Europe/Budapest | 28 | Europe/Belgrade | (GMT+01:00) Belgrade, Bratislava, Budapest, Ljubljana, Prague |
| Europe/Busingen | 27 | Europe/Berlin | (GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna |
| Europe/Chisinau | 35 | Europe/Riga | (GMT+02:00) Helsinki, Kiev, Riga, Sofia, Tallinn, Vilnius |
| Europe/Copenhagen | 27 | Europe/Berlin | (GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna |
| Europe/Dublin | 26 | Europe/London | (GMT) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London |
| Europe/Gibraltar | 27 | Europe/Berlin | (GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna |
| Europe/Guernsey | 26 | Europe/London | (GMT) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London |
| Europe/Helsinki | 35 | Europe/Riga | (GMT+02:00) Helsinki, Kiev, Riga, Sofia, Tallinn, Vilnius |
| Europe/Isle_of_Man | 26 | Europe/London | (GMT) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London |
| Europe/Istanbul | 32 | Europe/Istanbul | (GMT+02:00) Athens, Bucharest, Istanbul |
| Europe/Jersey | 26 | Europe/London | (GMT) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London |
| Europe/Kaliningrad | 34 | Africa/Harare | (GMT+02:00) Harare, Pretoria |
| Europe/Kiev | 35 | Europe/Riga | (GMT+02:00) Helsinki, Kiev, Riga, Sofia, Tallinn, Vilnius |
| Europe/Kirov | 41 | Europe/Moscow | (GMT+03:00) Moscow, St. Petersburg, Volgograd |
| Europe/Lisbon | 26 | Europe/London | (GMT) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London |
| Europe/Ljubljana | 28 | Europe/Belgrade | (GMT+01:00) Belgrade, Bratislava, Budapest, Ljubljana, Prague |
| Europe/London | 26 | Europe/London | (GMT) Greenwich Mean Time : Dublin, Edinburgh, Lisbon, London |
| Europe/Luxembourg | 29 | Europe/Paris | (GMT+01:00) Brussels, Copenhagen, Madrid, Paris |
| Europe/Madrid | 29 | Europe/Paris | (GMT+01:00) Brussels, Copenhagen, Madrid, Paris |
| Europe/Malta | 27 | Europe/Berlin | (GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna |
| Europe/Mariehamn | 35 | Europe/Riga | (GMT+02:00) Helsinki, Kiev, Riga, Sofia, Tallinn, Vilnius |
| Europe/Minsk | 37 | Europe/Minsk | (GMT+02:00) Minsk |
| Europe/Monaco | 29 | Europe/Paris | (GMT+01:00) Brussels, Copenhagen, Madrid, Paris |
| Europe/Moscow | 41 | Europe/Moscow | (GMT+03:00) Moscow, St. Petersburg, Volgograd |
| Europe/Oslo | 27 | Europe/Berlin | (GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna |
| Europe/Paris | 29 | Europe/Paris | (GMT+01:00) Brussels, Copenhagen, Madrid, Paris |
| Europe/Podgorica | 28 | Europe/Belgrade | (GMT+01:00) Belgrade, Bratislava, Budapest, Ljubljana, Prague |
| Europe/Prague | 28 | Europe/Belgrade | (GMT+01:00) Belgrade, Bratislava, Budapest, Ljubljana, Prague |
| Europe/Riga | 35 | Europe/Riga | (GMT+02:00) Helsinki, Kiev, Riga, Sofia, Tallinn, Vilnius |
| Europe/Rome | 27 | Europe/Berlin | (GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna |
| Europe/Samara | 44 | Asia/Muscat | (GMT+04:00) Abu Dhabi, Muscat |
| Europe/San_Marino | 27 | Europe/Berlin | (GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna |
| Europe/Sarajevo | 30 | Europe/Belgrade | (GMT+01:00) Sarajevo, Skopje, Warsaw, Zagreb |
| Europe/Saratov | 44 | Asia/Muscat | (GMT+04:00) Abu Dhabi, Muscat |
| Europe/Simferopol | 41 | Europe/Moscow | (GMT+03:00) Moscow, St. Petersburg, Volgograd |
| Europe/Skopje | 30 | Europe/Belgrade | (GMT+01:00) Sarajevo, Skopje, Warsaw, Zagreb |
| Europe/Sofia | 35 | Europe/Riga | (GMT+02:00) Helsinki, Kiev, Riga, Sofia, Tallinn, Vilnius |
| Europe/Stockholm | 27 | Europe/Berlin | (GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna |
| Europe/Tallinn | 35 | Europe/Riga | (GMT+02:00) Helsinki, Kiev, Riga, Sofia, Tallinn, Vilnius |
| Europe/Tirane | 28 | Europe/Belgrade | (GMT+01:00) Belgrade, Bratislava, Budapest, Ljubljana, Prague |
| Europe/Ulyanovsk | 44 | Asia/Muscat | (GMT+04:00) Abu Dhabi, Muscat |
| Europe/Vaduz | 27 | Europe/Berlin | (GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna |
| Europe/Vatican | 27 | Europe/Berlin | (GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna |
| Europe/Vienna | 27 | Europe/Berlin | (GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna |
| Europe/Vilnius | 35 | Europe/Riga | (GMT+02:00) Helsinki, Kiev, Riga, Sofia, Tallinn, Vilnius |
| Europe/Volgograd | 41 | Europe/Moscow | (GMT+03:00) Moscow, St. Petersburg, Volgograd |
| Europe/Warsaw | 30 | Europe/Belgrade | (GMT+01:00) Sarajevo, Skopje, Warsaw, Zagreb |
| Europe/Zagreb | 30 | Europe/Belgrade | (GMT+01:00) Sarajevo, Skopje, Warsaw, Zagreb |
| Europe/Zurich | 27 | Europe/Berlin | (GMT+01:00) Amsterdam, Berlin, Bern, Rome, Stockholm, Vienna |
| Indian/Antananarivo | 42 | Africa/Nairobi | (GMT+03:00) Nairobi |
| Indian/Chagos | 53 | Asia/Kathmandu (closest) | (GMT+05:45) Kathmandu |
| Indian/Christmas | 57 | Asia/Bangkok | (GMT+07:00) Bangkok, Hanoi, Jakarta |
| Indian/Cocos | 56 | Asia/Rangoon | (GMT+06:30) Yangon (Rangoon) |
| Indian/Comoro | 42 | Africa/Nairobi | (GMT+03:00) Nairobi |
| Indian/Kerguelen | 49 | Asia/Yekaterinburg | (GMT+05:00) Ekaterinburg |
| Indian/Mahe | 44 | Asia/Muscat | (GMT+04:00) Abu Dhabi, Muscat |
| Indian/Maldives | 49 | Asia/Yekaterinburg | (GMT+05:00) Ekaterinburg |
| Indian/Mauritius | 44 | Asia/Muscat | (GMT+04:00) Abu Dhabi, Muscat |
| Indian/Mayotte | 42 | Africa/Nairobi | (GMT+03:00) Nairobi |
| Indian/Reunion | 44 | Asia/Muscat | (GMT+04:00) Abu Dhabi, Muscat |
| Pacific/Apia | 0 | Pacific/Apia | (GMT-11:00) Midway Island, Samoa |
| Pacific/Auckland | 75 | Pacific/Auckland | (GMT+12:00) Auckland, Wellington |
| Pacific/Bougainville | 74 | Pacific/Guadalcanal | (GMT+11:00) Magadan, Solomon Islands, New Caledonia |
| Pacific/Chatham | 77 | Pacific/Tongatapu (closest) | (GMT+13:00) Nuku'alofa |
| Pacific/Easter | 7 | America/Regina (closest) | (GMT-06:00) Saskatchewan |
| Pacific/Efate | 74 | Pacific/Guadalcanal | (GMT+11:00) Magadan, Solomon Islands, New Caledonia |
| Pacific/Enderbury | 0 | Pacific/Apia | (GMT-11:00) Midway Island, Samoa |
| Pacific/Fakaofo | 0 | Pacific/Apia | (GMT-11:00) Midway Island, Samoa |
| Pacific/Fiji | 76 | Pacific/Fiji | (GMT+12:00) Fiji Islands, Kamchatka, Marshall Islands |
| Pacific/Funafuti | 76 | Pacific/Fiji | (GMT+12:00) Fiji Islands, Kamchatka, Marshall Islands |
| Pacific/Galapagos | 10 | America/Costa_Rica | (GMT-06:00) Central America |
| Pacific/Gambier | 2 | America/Anchorage (closest) | (GMT-09:00) Alaska |
| Pacific/Guadalcanal | 74 | Pacific/Guadalcanal | (GMT+11:00) Magadan, Solomon Islands, New Caledonia |
| Pacific/Guam | 71 | Pacific/Guam | (GMT+10:00) Guam, Port Moresby |
| Pacific/Honolulu | 1 | Pacific/Honolulu | (GMT-10:00) Hawaii |
| Pacific/Kiritimati | 77 | Pacific/Tongatapu (closest) | (GMT+13:00) Nuku'alofa |
| Pacific/Kosrae | 74 | Pacific/Guadalcanal | (GMT+11:00) Magadan, Solomon Islands, New Caledonia |
| Pacific/Kwajalein | 76 | Pacific/Fiji | (GMT+12:00) Fiji Islands, Kamchatka, Marshall Islands |
| Pacific/Majuro | 76 | Pacific/Fiji | (GMT+12:00) Fiji Islands, Kamchatka, Marshall Islands |
| Pacific/Marquesas | 1 | Pacific/Honolulu (closest) | (GMT-10:00) Hawaii |
| Pacific/Midway | 1 | Pacific/Honolulu (closest) | (GMT-10:00) Hawaii |
| Pacific/Nauru | 76 | Pacific/Fiji | (GMT+12:00) Fiji Islands, Kamchatka, Marshall Islands |
| Pacific/Niue | 1 | Pacific/Honolulu (closest) | (GMT-10:00) Hawaii |
| Pacific/Norfolk | 74 | Pacific/Guadalcanal (closest) | (GMT+11:00) Magadan, Solomon Islands, New Caledonia |
| Pacific/Noumea | 74 | Pacific/Guadalcanal | (GMT+11:00) Magadan, Solomon Islands, New Caledonia |
| Pacific/Pago_Pago | 1 | Pacific/Honolulu (closest) | (GMT-10:00) Hawaii |
| Pacific/Palau | 64 | Asia/Tokyo | (GMT+09:00) Osaka, Sapporo, Tokyo |
| Pacific/Pitcairn | 2 | America/Anchorage (closest) | (GMT-09:00) Alaska |
| Pacific/Ponape | 74 | Pacific/Guadalcanal | (GMT+11:00) Magadan, Solomon Islands, New Caledonia |
| Pacific/Port_Moresby | 71 | Pacific/Guam | (GMT+10:00) Guam, Port Moresby |
| Pacific/Rarotonga | 1 | Pacific/Honolulu | (GMT-10:00) Hawaii |
| Pacific/Saipan | 71 | Pacific/Guam | (GMT+10:00) Guam, Port Moresby |
| Pacific/Tahiti | 1 | Pacific/Honolulu | (GMT-10:00) Hawaii |
| Pacific/Tarawa | 76 | Pacific/Fiji | (GMT+12:00) Fiji Islands, Kamchatka, Marshall Islands |
| Pacific/Tongatapu | 77 | Pacific/Tongatapu | (GMT+13:00) Nuku'alofa |
| Pacific/Truk | 71 | Pacific/Guam | (GMT+10:00) Guam, Port Moresby |
| Pacific/Wake | 76 | Pacific/Fiji | (GMT+12:00) Fiji Islands, Kamchatka, Marshall Islands |
| Pacific/Wallis | 76 | Pacific/Fiji | (GMT+12:00) Fiji Islands, Kamchatka, Marshall Islands |
