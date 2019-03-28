package com.detroitlabs.FinalProject.controller;

import com.detroitlabs.FinalProject.model.*;
import com.detroitlabs.FinalProject.service.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.ui.ModelMap;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.ArrayList;
import java.util.List;

@Controller
public class TripController {

    @Autowired
    private YelpService yelpService;

    @Autowired
    private GeoCodingService geoCodingService;

    @Autowired
   private DirectionsService directionsService;

    @Autowired
    PictureWrapper pictureWrapper;

    @Autowired
    PictureService pictureService;

//    @Autowired
//    WeatherService weatherService;

    @Value("${GOOGLE_MAPS_KEY}")
    private String googleMapsKey;

    private ArrayList<TripToAdd> allPlaces= new ArrayList<>();
    private static final String AJAX_HEADER_NAME = "X-Requested-With";
    private static final String AJAX_HEADER_VALUE = "XMLHttpRequest";


    @RequestMapping("/")
    public String displayHomePage(Model model){
        model.addAttribute("blankTrip", new BlankTrip());
        return "index";
    }

    //WORKS WITH REQUEST PARAMS
    @PostMapping("/addToMyTrip/{businessName}")
    public String addPlaceToMyTripList(@PathVariable(name="businessName") String businessName,HttpServletRequest request, Model model){
        allPlaces.add(new TripToAdd(businessName));
        model.addAttribute("allPlaces", allPlaces);

        if(AJAX_HEADER_VALUE.equals(request.getHeader(AJAX_HEADER_NAME))){
            return "showTrip :: tripList";
        }else{
            return "showTrip";
        }
    }

//ATTEMPT WITH REQUEST BODY
//    @PostMapping("/addToMyTrip")
//    public String addPlaceToMyTripList(@RequestBody TripToAdd tripToAdd, HttpServletRequest request, Model model){
//        allPlaces.add(tripToAdd);
//        model.addAttribute("allPlaces", allPlaces);
//
//        if(AJAX_HEADER_VALUE.equals(request.getHeader(AJAX_HEADER_NAME))){
//            return "showTrip :: tripList";
//        }else{
//            return "showTrip";
//        }
    @RequestMapping("/hotel")
public String testHotelPage(){
        return"hotels";
    }
//    @RequestMapping("/index")
//    public String testPage(){
//        return "index";
//    }


    @RequestMapping("/showtrip")
    public String displayTripPage(@ModelAttribute StepCoordinates gaslongitude, @ModelAttribute StepCoordinates gaslatitude, @ModelAttribute BlankTrip blankTrip, ModelMap modelMap, Model model){
       String tripStart = blankTrip.getStart();
       String tripEnd = blankTrip.getEnd();
       modelMap.put("tripStart", tripStart);
       modelMap.put("tripEnd", tripEnd);

       //Google Directions

       DirectionSet directionSet =  directionsService.fetchDirectionSetForRoute(tripStart, tripEnd);
       ArrayList<Step> tripSteps = directionSet.getRoutes().get(0).getStepRepository().get(0).getSteps();

       ArrayList<String> cityNames = getCityNamesByStepCoordinates(tripSteps);

       ArrayList<String> filteredCityNames = filterDuplicateCities(cityNames);

       TripCityPlaces tripCityPlaces = generateTripCityPlaces(filteredCityNames, yelpService);

       allPlaces.add(new TripToAdd("Detroit", "Detroit Labs"));
       model.addAttribute("allPlaces", allPlaces);

       modelMap.put("tripSteps", tripSteps);
       modelMap.put("allCityNames", cityNames);
       modelMap.put("filteredCityNames",filteredCityNames);
       modelMap.put("tripCityPlaces", tripCityPlaces.getTripCityPlaces());
       modelMap.put("googleMapsKey", googleMapsKey);

       //Gas Station Info

        gaslongitude = directionSet.getRoutes().get(0).getStepRepository().get(0).getSteps().get(0).getEndLocation();
       gaslatitude = directionSet.getRoutes().get(0).getStepRepository().get(0).getSteps().get(0).getEndLocation();


      PictureWrapper pictureWrapper = pictureService.fetchPictureByCity(tripEnd);
      List<PictureResults> pictureResults = pictureWrapper.getPictureResults();
      modelMap.put("pictureResults", pictureResults);



      //Weather In

//        Forecast forecast = weatherService.fetchWeatherData(gaslongitude.getLongitude(), gaslatitude.getLatitude());
//        ArrayList<WeatherData> mainWeatherData = forecast.getWeatherData();
//        modelMap.put("mainWeatherData", mainWeatherData);


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

}
