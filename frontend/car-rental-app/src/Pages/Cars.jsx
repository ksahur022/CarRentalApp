import React, { useState } from 'react';
import CarCards from '../Components/CarCards'; // Assuming this exists
import Filter from '../Components/Filter'; // Import the Filter component
import '../Css/Cars.css'; // Ensure you have the relevant CSS
import Footer from '../Components/Footer'; // Ensure Footer is imported

function Cars() {
    const cars = [
        { name: 'Nissan Z 2024', location: 'Ukraine, Kyiv', price: 550 },
        { name: 'BMW X5 2023', location: 'Germany, Berlin', price: 600 },
        { name: 'Audi A4 2022', location: 'France, Paris', price: 580 },
        { name: 'Ford Mustang 2024', location: 'USA, New York', price: 650 },
        { name: 'Honda Civic 2023', location: 'Japan, Tokyo', price: 300 },
        { name: 'Tesla Model 3 2022', location: 'USA, San Francisco', price: 750 },
        { name: 'Porsche 911 2023', location: 'Germany, Stuttgart', price: 1200 },
        { name: 'Chevrolet Camaro 2024', location: 'USA, Los Angeles', price: 650 },
        { name: 'Mazda 3 2022', location: 'Japan, Tokyo', price: 350 },
        { name: 'Hyundai i30 2023', location: 'South Korea, Seoul', price: 400 },
        { name: 'Kia Forte 2023', location: 'South Korea, Busan', price: 370 },
        { name: 'Subaru WRX 2024', location: 'USA, Seattle', price: 700 },
        { name: 'Volkswagen Golf 2023', location: 'Germany, Hamburg', price: 470 },
        { name: 'Toyota Corolla 2022', location: 'Japan, Toyota City', price: 320 },
        { name: 'Lexus ES 2023', location: 'Japan, Nagoya', price: 850 },
        { name: 'Nissan Altima 2023', location: 'USA, Nashville', price: 340 },
        { name: 'Nissan Z 2024', location: 'Ukraine, Kyiv', price: 550 },
        { name: 'BMW X5 2023', location: 'Germany, Berlin', price: 600 },
        { name: 'Audi A4 2022', location: 'France, Paris', price: 580 },
        { name: 'Ford Mustang 2024', location: 'USA, New York', price: 650 },
        { name: 'Honda Civic 2023', location: 'Japan, Tokyo', price: 300 },
        { name: 'Tesla Model 3 2022', location: 'USA, San Francisco', price: 750 },
        { name: 'Porsche 911 2023', location: 'Germany, Stuttgart', price: 1200 },
        { name: 'Chevrolet Camaro 2024', location: 'USA, Los Angeles', price: 650 },
        { name: 'Mazda 3 2022', location: 'Japan, Tokyo', price: 350 },
        { name: 'Hyundai i30 2023', location: 'South Korea, Seoul', price: 400 },
        { name: 'Kia Forte 2023', location: 'South Korea, Busan', price: 370 },
        { name: 'Subaru WRX 2024', location: 'USA, Seattle', price: 700 },
        { name: 'Volkswagen Golf 2023', location: 'Germany, Hamburg', price: 470 },
    ];

    const [currentPage, setCurrentPage] = useState(1);
    const carsPerPage = 16; // Cars per page
    const totalCars = cars.length;
    const totalPages = Math.ceil(totalCars / carsPerPage);

    // Calculate which cars to display on the current page
    const indexOfLastCar = currentPage * carsPerPage;
    const indexOfFirstCar = indexOfLastCar - carsPerPage;
    const currentCars = cars.slice(indexOfFirstCar, indexOfLastCar);

    const handlePageChange = (pageNumber) => {
        if (pageNumber > 0 && pageNumber <= totalPages) {
            setCurrentPage(pageNumber);
        }
    };

    return (
        <div className='cars-page' id="cars-page">
            <div className="page-title" id="cars-page-title">
                <h1>Choose a Car for Rental</h1>
            </div>
            <Filter />
            <div className="car-cards-container" id="car-cards-container">
                {currentCars.map((car, index) => (
                    <CarCards
                        key={index}
                        carName={car.name}
                        location={car.location}
                        price={car.price}
                    />
                ))}
            </div>
            <div className="pagination" id="cars-pagination">
                {/* Display page numbers with a current page indicator */}
                {Array.from({ length: totalPages }, (_, i) => (
                    <button
                        key={i + 1}
                        onClick={() => handlePageChange(i + 1)}
                        className={currentPage === i + 1 ? "active" : ""}
                    >
                        {i + 1}
                    </button>
                ))}
                {/* Next Page Arrow */}
                <button onClick={() => handlePageChange(currentPage + 1)} disabled={currentPage === totalPages}>
                    ➡️ {/* Arrow for the next page */}
                </button>
            </div>
            <div className='foot-container' id="foot-container">
                <Footer />
            </div>
        </div>
    );
}

export default Cars;