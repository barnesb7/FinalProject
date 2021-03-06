package com.detroitlabs.FinalProject.controller;

import com.detroitlabs.FinalProject.model.*;
import com.detroitlabs.FinalProject.service.DirectionsService;
import com.detroitlabs.FinalProject.service.GeoCodingService;
import com.detroitlabs.FinalProject.service.TripService;
import com.detroitlabs.FinalProject.service.YelpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.ArrayList;
import java.util.List;

@Controller
public class TripController {

    @Autowired
    private Businesses businesses;

    @Autowired
    private BusinessInformation businessInformation;

    @Autowired
    private YelpService yelpService;

    @Autowired
    private Stations stations;

    @Autowired
    private TripService tripService;

    @Autowired
    private GeoCodingService geoCodingService;

    @Autowired
   private DirectionsService directionsService;

    @Autowired
    StationsWrapper stationsWrapper;

    @Value("${GOOGLE_MAPS_KEY}")
    private String googleMapsKey;

    @RequestMapping("/")
    public String displayHomePage(Model model){
        model.addAttribute("blankTrip", new BlankTrip());
        return "bootstrapHome";
    }

    @RequestMapping("/showtrip")
    public String displayTripPage(@ModelAttribute BlankTrip blankTrip, ModelMap modelMap){
       String tripStart = blankTrip.getStart();
       String tripEnd = blankTrip.getEnd();
       modelMap.put("tripStart", tripStart);
       modelMap.put("tripEnd", tripEnd);

       //YELP

       Businesses barBusinesses = yelpService.fetchYelpMostRatedBars(blankTrip.getEnd());
       modelMap.put("barBusinesses",barBusinesses.getBusinesses());

       Businesses restaurantBusinesses = yelpService.fetchYelpMostRatedRestaurants(blankTrip.getEnd());
       modelMap.put("restaurantBusinesses", restaurantBusinesses.getBusinesses());

       Businesses hotelBusinesses = yelpService.fetchYelpMostRatedHotels(blankTrip.getEnd());
       modelMap.put("hotelBusinesses", hotelBusinesses.getBusinesses());

       Businesses entertainmentBusinesses = yelpService.fetchYelpMostRatedEntertainment(blankTrip.getEnd());
       modelMap.put("entertainmentBusinesses", entertainmentBusinesses.getBusinesses());

       //Google Directions

       DirectionSet directionSet =  directionsService.fetchDirectionSetForRoute(tripStart, tripEnd);
       ArrayList<Step> tripSteps = directionSet.getRoutes().get(0).getStepRepository().get(0).getSteps();

       ArrayList<String> cityNames = getCityNamesByStepCoordinates(tripSteps);

       ArrayList<String> filteredCityNames = filterDuplicateCities(cityNames);

       TripCityPlaces tripCityPlaces = generateTripCityPlaces(filteredCityNames, yelpService);

       modelMap.put("tripSteps", tripSteps);
       modelMap.put("allCityNames", cityNames);
       modelMap.put("filteredCityNames",filteredCityNames);
       modelMap.put("tripCityPlaces", tripCityPlaces.getTripCityPlaces());
       modelMap.put("googleMapsKey", googleMapsKey);

        return "showtrip";
    }

    public TripCityPlaces generateTripCityPlaces(ArrayList<String> filteredCities, YelpService yelpService){
        TripCityPlaces tripCityPlaces = new TripCityPlaces();

        for (String cityName : filteredCities){
            tripCityPlaces.addTripCityPlace(new CityPlaces(cityName,yelpService));
        }

        return tripCityPlaces;
    }

    public ArrayList<String> filterDuplicateCities(ArrayList<String> cities){

        ArrayList<String> filteredCities = new ArrayList<>();

        for (String city : cities){
            if (!filteredCities.contains(city)){
                filteredCities.add(city);
            }
        }

        return filteredCities;
    }



    private ArrayList<String> getCityNamesByStepCoordinates(ArrayList<Step> tripSteps){

        ArrayList<String> allCities = new ArrayList<>();

        for(Step step: tripSteps){

           String latAndLong = step.getStartLocation().getFormattedLatAndLong();

           GeoLocationCityInfo cityRawJSONInfo = geoCodingService.fetchCityInfoByCoordinate(latAndLong);

           String cityName = cityRawJSONInfo.getPlusCode().parseCityNameFromCode();

           allCities.add(cityName);
        }

        return allCities;
    }


//putting in a comment
//    @RequestMapping("/")
//    @ResponseBody
//    public String displayAllIssues(ModelMap modelMap){
//        StationsWrapper stationsWrapper = tripService.DisplayAllGasStation();
//      List<Stations> allGasStations = stationsWrapper.getStations();
//        modelMap.put("allGasStations", allGasStations);
//        return allGasStations.toString();
//    }


}
