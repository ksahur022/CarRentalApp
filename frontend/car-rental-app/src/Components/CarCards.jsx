import React from 'react';
import PropTypes from 'prop-types';
import carImage from '../assets/cardCar.png';
import '../Css/CarCards.css'
import { NavLink } from 'react-router-dom';

function CarCards({ carName, location, price }) {
    return (

        <div className="card-container">
            <div className="img-container">
                <img src={carImage} alt={carName} />
            </div>
            <div className="car-details">
                <h2>{carName}</h2>
                <p>{location}</p>
                <NavLink to="/booking" className="car-card-book-button"> {/* Use NavLink for navigation */}
                    Book the car - ${price}/day
                </NavLink>
            </div>
        </div>

    );
}

// Add prop types for validation
CarCards.propTypes = {
    carName: PropTypes.string.isRequired,
    location: PropTypes.string.isRequired,
    price: PropTypes.number.isRequired,
};

export default CarCards;