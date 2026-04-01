package org.manu.learning.flights;

import org.manu.learning.flights.flights.Travel;
import org.manu.learning.flights.weather.Weather;

public record Page(Travel travel, Weather weather) {
}
