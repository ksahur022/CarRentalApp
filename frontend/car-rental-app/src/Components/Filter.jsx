import React, { useState } from 'react';
import '../Css/Filter.css';

function Filter({ handleFilter, handleClearFilters }) {
    const [formData, setFormData] = useState({
        pickupLocation: "",
        dropOffLocation: "",
        pickupDate: "",
        dropOffDate: "",
        carCategory: "",
        gearboxType: "",
        engineType: "",
        priceRange: 500
    });

    const handleChange = (e) => {
        const { name, value } = e.target;
        setFormData((prevData) => ({
            ...prevData,
            [name]: value
        }));
    };

    const onFilterApply = () => {
        const params = new URLSearchParams({
            pickupLocationId: formData.pickupLocation,
            dropOffLocationId: formData.dropOffLocation,
            pickupDateTime: new Date(formData.pickupDate).toISOString(),
            dropOffDateTime: new Date(formData.dropOffDate).toISOString(),
            category: formData.carCategory,
            gearBoxType: formData.gearboxType,
            fuelType: formData.engineType,
            minPrice: formData.priceRange - 50,
            maxPrice: formData.priceRange
        });

        handleFilter(params.toString());
    };

    const onClearFilters = () => {
        setFormData({
            pickupLocation: "",
            dropOffLocation: "",
            pickupDate: "",
            dropOffDate: "",
            carCategory: "",
            gearboxType: "",
            engineType: "",
            priceRange: 500
        });
        handleClearFilters();
    };

    return (
        <div className="form-container" id="form-container">
            <div className="clear-filters" id="clear-filters">
                <button className='clear-all-filter-btn' onClick={onClearFilters}>
                    Clear All Filters
                </button>
            </div>
            <div className="form-grid" id="first-row">
                <div className="form-item" id="pickup-location-item">
                    <label htmlFor="pickupLocation">Pick-up Location:</label>
                    <select
                        id="pickupLocation"
                        name="pickupLocation"
                        value={formData.pickupLocation}
                        onChange={handleChange}
                    >
                        <option value="">--Select Location--</option>
                        <option value="8bda2022-792c-4b67-b6b3-d1cfddc8f40a">Location 1</option>
                        <option value="8bda2023-792c-4b67-b6b3-d1cfddc8f40a">Location 2</option>
                        <option value="8bda2024-792c-4b67-b6b3-d1cfddc8f40a">Location 3</option>
                    </select>
                </div>

                <div className="form-item" id="drop-off-location-item">
                    <label htmlFor="dropOffLocation">Drop-Off Location:</label>
                    <select
                        id="dropOffLocation"
                        name="dropOffLocation"
                        value={formData.dropOffLocation}
                        onChange={handleChange}
                    >
                        <option value="">--Select Location--</option>
                        <option value="8bda2022-792c-4b67-b6b3-d1cfddc8f40a">Location 1</option>
                        <option value="8bda2023-792c-4b67-b6b3-d1cfddc8f40a">Location 2</option>
                        <option value="8bda2024-792c-4b67-b6b3-d1cfddc8f40a">Location 3</option>
                    </select>
                </div>

                <div className="form-item" id="pickup-date-item">
                    <label htmlFor="pickupDate">Pick-up Date:</label>
                    <input
                        type="date"
                        id="pickupDate"
                        name="pickupDate"
                        value={formData.pickupDate}
                        onChange={handleChange}
                    />
                </div>

                <div className="form-item" id="drop-off-date-item">
                    <label htmlFor="dropOffDate">Drop-Off Date:</label>
                    <input
                        type="date"
                        id="dropOffDate"
                        name="dropOffDate"
                        value={formData.dropOffDate}
                        onChange={handleChange}
                    />
                </div>
            </div>

            <div className="form-grid second-row" id="second-row">
                <div className="form-item narrow" id="car-category-item">
                    <label htmlFor="carCategory">Car Category:</label>
                    <select
                        id="carCategory"
                        name="carCategory"
                        value={formData.carCategory}
                        onChange={handleChange}
                    >
                        <option value="">--Select Category--</option>
                        <option value="ECONOMY">Economy</option>
                        <option value="LUXURY">Luxury</option>
                        <option value="SUV">SUV</option>
                    </select>
                </div>

                <div className="form-item narrow" id="gearbox-type-item">
                    <label htmlFor="gearboxType">Gearbox Type:</label>
                    <select
                        id="gearboxType"
                        name="gearboxType"
                        value={formData.gearboxType}
                        onChange={handleChange}
                    >
                        <option value="">--Select Gearbox--</option>
                        <option value="MANUAL">Manual</option>
                        <option value="AUTOMATIC">Automatic</option>
                    </select>
                </div>

                <div className="form-item narrow" id="engine-type-item">
                    <label htmlFor="engineType">Type of Engine:</label>
                    <select
                        id="engineType"
                        name="engineType"
                        value={formData.engineType}
                        onChange={handleChange}
                    >
                        <option value="">--Select Engine Type--</option>
                        <option value="PETROL">Petrol</option>
                        <option value="DIESEL">Diesel</option>
                        <option value="ELECTRIC">Electric</option>
                    </select>
                </div>

                <div className="form-item-narrow-slidebar" id="price-range-item">
                    <div className="slider-container">
                        <label htmlFor="priceRange">Price Range: ${formData.priceRange}</label>
                        <input
                            type="range"
                            id="priceRange"
                            name="priceRange"
                            min="100"
                            max="1000"
                            step="50"
                            value={formData.priceRange}
                            onChange={handleChange}
                        />
                    </div>
                </div>

                <div className="form-item narrow" id="find-car-item">
                    <button className='find-a-car-btn' onClick={onFilterApply}>
                        Find a Car
                    </button>
                </div>
            </div>
        </div>
    );
}

export default Filter;
