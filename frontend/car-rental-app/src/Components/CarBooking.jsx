import React, { useState, useEffect } from 'react';
import DatePicker from 'react-datepicker';
import "react-datepicker/dist/react-datepicker.css";
import '../Css/CarBooking.css';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../Utils/AuthContext';

import carImg from '../assets/cardCar.png';
import automaticIcon from '../assets/Logos/manual-gearbox.png';
import petrolIcon from '../assets/Logos/petrol.png';
import seatsIcon from '../assets/Logos/people.png';
import climateControlIcon from '../assets/Logos/climate.png';
import range from '../assets/Logos/range.png';
import turbo from '../assets/Logos/turbo.png';

const carImages = [
  { id: 1, src: carImg, alt: "Car Image 1" },
  { id: 2, src: carImg, alt: "Car Image 2" },
  { id: 3, src: carImg, alt: "Car Image 3" },
  { id: 4, src: carImg, alt: "Car Image 4" }
];

const feedbackData = [
  {
    id: 1,
    name: "Sarah L.",
    avatar: "https://randomuser.me/api/portraits/women/44.jpg",
    rating: 5,
    text: "Fantastic service from start to finish! The booking process was smooth, and the staff was incredibly helpful. The car was clean and made my trip much more enjoyable. Highly recommend!",
    date: "01.11.2024"
  },
  {
    id: 2,
    name: "Ahmed K.",
    avatar: "https://randomuser.me/api/portraits/men/12.jpg",
    rating: 4,
    text: "The car rental process was seamless, and the car was practically brand new. Loved the variety of vehicles and the flexibility for pick-up and drop-off.",
    date: "28.10.2024"
  },
  {
    id: 3,
    name: "David P.",
    avatar: "https://randomuser.me/api/portraits/men/33.jpg",
    rating: 5,
    text: "The best car rental experience I've had. Quick booking, excellent service, and a well-maintained vehicle. Will use them again for future trips!",
    date: "14.10.2024"
  }
];

function CarBooking({ onClose }) {
  const [car, setCar] = useState(null);
  const [sortBy, setSortBy] = useState('newest');
  const [currentPage, setCurrentPage] = useState(1);
  const [startDate, setStartDate] = useState(new Date());
  const [endDate, setEndDate] = useState(new Date());
  const feedbackPerPage = 3;

  useEffect(() => {
    const fetchCar = async () => {
      try {
        const carId = 'd4e5f6a7-b8c9-7d6e-1f0a-2b3c4d5e6f7a';
        const response = await fetch(`https://j5u3343jca.execute-api.us-east-2.amazonaws.com/api/cars/${carId}`);
        const data = await response.json();
        setCar(data);
      } catch (error) {
        console.error('Failed to fetch car data:', error);
      }
    };

    fetchCar();
  }, []);

  const sortedFeedback = [...feedbackData].sort((a, b) => {
    if (sortBy === 'newest') {
      return new Date(b.date.split('.').reverse().join('-')) - new Date(a.date.split('.').reverse().join('-'));
    } else if (sortBy === 'oldest') {
      return new Date(a.date.split('.').reverse().join('-')) - new Date(b.date.split('.').reverse().join('-'));
    } else if (sortBy === 'highest') {
      return b.rating - a.rating;
    } else if (sortBy === 'lowest') {
      return a.rating - b.rating;
    }
    return 0;
  });

  const { isLoggedIn } = useAuth();
  const navigate = useNavigate();

  const handleBooking = () => {
    if (!isLoggedIn) {
      alert('You must be logged in to book a car!');
      navigate('/login')
      return;
    }
    navigate('/reservation');
  };

  const indexOfLastFeedback = currentPage * feedbackPerPage;
  const indexOfFirstFeedback = indexOfLastFeedback - feedbackPerPage;
  const currentFeedback = sortedFeedback.slice(indexOfFirstFeedback, indexOfLastFeedback);
  const totalPages = Math.ceil(feedbackData.length / feedbackPerPage);

  const handlePageChange = (pageNumber) => {
    setCurrentPage(pageNumber);
  };

  if (!car) {
    return <div className="car-booking-main-container">Loading car details...</div>;
  }

  return (
    <div className="car-booking-main-container">
      <div className="close-container">
        <button className="close-button" onClick={onClose}>×</button>
      </div>

      <div className="main-container">
        <div className="car-info-container">
          <div className="car-info-container-2">
            <div className="side-images-container">
              {carImages.map(image => (
                <img key={image.id} src={image.src} alt={image.alt} className="side-image" />
              ))}
            </div>

            <div className="main-image-container">
              <img src={carImg} alt="Main Car" className="main-car-image" />
              <span className="available-tag">Available</span>
            </div>

            <div className="car-details-container">
              <div className="details-card">
                <div className="car-header">
                  <h2>{car.model}</h2>
                  <p className="rating">{car.carRating} ★</p>
                </div>
                <p className="location">{car.location || "Russia"}</p>
                <div className="border-bottom"></div>

                <div className="specs-container">
                  <div className="specs-row">
                    <div className="spec-item">
                      <img src={automaticIcon} alt="Gearbox" className="spec-icon" />
                      <p>{car.gearBoxType}</p>
                    </div>
                    <div className="spec-item">
                      <img src={petrolIcon} alt="Fuel Type" className="spec-icon" />
                      <p>{car.fuelType}</p>
                    </div>
                    <div className="spec-item">
                      <img src={range} alt="Range" className="spec-icon" />
                      <p>{car.fuelConsumption}</p>
                    </div>
                  </div>
                  <div className="specs-row">
                    <div className="spec-item">
                      <img src={turbo} alt="Turbo" className="spec-icon" />
                      <p>{car.engineCapacity}</p>
                    </div>
                    <div className="spec-item">
                      <img src={seatsIcon} alt="Seats" className="spec-icon" />
                      <p>{car.passengerCapacity} Seats</p>
                    </div>
                    <div className="spec-item">
                      <img src={climateControlIcon} alt="Climate Control" className="spec-icon" />
                      <p>{car.climateControlOption}</p>
                    </div>
                  </div>
                </div>

                <div className="booking-dates">
                  <div className="date-time-picker">
                    <div className="date-picker-container">
                      <label>Start Date:</label>
                      <DatePicker
                        selected={startDate}
                        onChange={(date) => setStartDate(date)}
                        className="date-picker"
                        dateFormat="dd/MM/yyyy"
                      />
                    </div>
                    <div className="date-picker-container">
                      <label>End Date:</label>
                      <DatePicker
                        selected={endDate}
                        onChange={(date) => setEndDate(date)}
                        className="date-picker"
                        dateFormat="dd/MM/yyyy"
                      />
                    </div>
                  </div>
                </div>
                <button className="book-button" onClick={handleBooking}>
                  ${car.pricePerDay}/day - Book
                </button>

              </div>
            </div>
          </div>
        </div>

        <div className="car-booking-feedback-container">
          <div className="feedback-header-top">
            <h3 className="feedback-text">Feedback</h3>
            <div className="sort-container">
              <label className="sort-by-text">Sort by</label>
              <select
                className="sort-by-values"
                value={sortBy}
                onChange={(e) => setSortBy(e.target.value)}
              >
                <option value="newest">The newest</option>
                <option value="oldest">The oldest</option>
                <option value="highest">Highest rating</option>
                <option value="lowest">Lowest rating</option>
              </select>
            </div>
          </div>

          <div className="feedback-list">
            {currentFeedback.map((feedback) => (
              <div className="feedback-card" key={feedback.id}>
                {/* First Row: Avatar and Name */}
                <div className="feedback-row feedback-top-row">
                  <div className="feedback-avatar">
                    <img
                      src={feedback.avatar}
                      alt={feedback.name}
                      className="user-avatar"
                    />
                  </div>
                  <div className="feedback-name">
                    <p className="user-name">{feedback.name}</p>
                  </div>
                </div>

                {/* Second Row: Rating and Feedback Text */}
                <div className="feedback-row feedback-middle-row">
                  <div className="feedback-rating">
                    <p>
                      {"★".repeat(feedback.rating)}
                      {"☆".repeat(5 - feedback.rating)}
                    </p>
                  </div>
                  <div className="feedback-text">
                    <p>{feedback.text}</p>
                  </div>
                </div>

                {/* Third Row: Date */}
                <div className="feedback-row feedback-bottom-row">
                  <p className="feedback-date">{feedback.date}</p>
                </div>
              </div>
            ))}
          </div>

          <div className="pagination-container">
            {Array.from({ length: totalPages }).map((_, index) => (
              <button
                key={index + 1}
                className={`page-button ${currentPage === index + 1 ? "active" : ""}`}
                onClick={() => handlePageChange(index + 1)}
              >
                {index + 1}
              </button>
            ))}
            <button
              className="page-button next"
              onClick={() =>
                currentPage < totalPages && handlePageChange(currentPage + 1)
              }
            >
              →
            </button>
          </div>
        </div>
      </div>
    </div>
  );
}

export default CarBooking;
