import React, { useState, useEffect } from 'react';
import CarCards from '../Components/CarCards';
import AboutUs from '../Components/AboutUs';
import Map from '../Components/Map';
import Filter from '../Components/Filter';
import '../Css/Home.css';
import Feedback from '../Components/FeedBAck';
import Faq from '../Components/Faq';
import Footer from '../Components/Footer';

function Home() {
    const cars = [
        { name: 'Nissan Z 2024', location: 'Ukraine, Kyiv', price: 550 },
        { name: 'BMW X5 2023', location: 'Germany, Berlin', price: 600 },
        { name: 'Audi A4 2022', location: 'France, Paris', price: 580 },
        { name: 'Ford Mustang 2024', location: 'USA, New York', price: 650 },
    ];

    const [filteredCars, setFilteredCars] = useState(cars); // Initialize with the cars list
    const feedbacks = [
        { carName: 'Toyota Corolla 2022', location: 'UK', rating: 4.5, comment: 'Excellent car, smooth driving experience!' },
        { carName: 'Honda Accord 2021', location: 'USA', rating: 4.0, comment: 'Very comfortable and reliable.' },
        { carName: 'Ford Focus 2020', location: 'Germany', rating: 4.2, comment: 'Great handling and performance.' },
        { carName: 'Chevrolet Malibu 2023', location: 'Canada', rating: 4.7, comment: 'Good fuel efficiency and comfort.' }
    ];

    const handleFilter = (query) => {
        // Parse the query string to get filter values
        const params = new URLSearchParams(query);

        const minPrice = parseInt(params.get('minPrice')) || 0;
        const maxPrice = parseInt(params.get('maxPrice')) || Infinity;
        const selectedCategory = params.get('category') || '';
        const selectedGearbox = params.get('gearBoxType') || '';

        // Filter the original cars array based on criteria
        const filtered = cars.filter(car => {
            const matchesPrice = car.price >= minPrice && car.price <= maxPrice;
            const matchesCategory = selectedCategory ? car.category === selectedCategory : true; // Example logic, adjust based on your actual data structure
            const matchesGearbox = selectedGearbox ? car.gearboxType === selectedGearbox : true; // Example logic

            return matchesPrice && matchesCategory && matchesGearbox;
        });

        setFilteredCars(filtered);
    };

    return (
        <div className="home-page">
            <div className="page-title">
                <h1>Choose a Car for Rental</h1>
            </div>
            <Filter handleFilter={handleFilter} />
            <div className="popular-cars-header">
                <h2>(Popular Cars)</h2>
            </div>
            <div className="car-cards-container">
                {filteredCars.map((car, index) => (
                    <CarCards
                        key={index}
                        carName={car.name}
                        location={car.location}
                        price={car.price}
                    />
                ))}
            </div>
            <div className="view-all-cars-header">
                <h2>View all cars</h2>
            </div>
            <AboutUs />
            <div className="map-container">
                <Map />
            </div>
            <div className="feedback-container">
                <Feedback feedbacks={feedbacks} />
            </div>
            <div className="faq-container">
                <Faq />
            </div>
            <div className="footer-container">
                <Footer />
            </div>
        </div>
    );
}

export default Home;