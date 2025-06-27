import React from 'react';
import '../Css/FeedBack.css'; // Import your CSS

function Feedback({ feedbacks }) {
    return (
        <div className="feedback-container" id="feedback-container">
            <h2 className="feedback-title" id="feedback-title">Recent Feedback</h2>
            <div className="feedback-items" id="feedback-items">
                {feedbacks.map((feedback, index) => (
                    <div key={index} className="feedback-item" id={`feedback-item-${index}`}>
                        <h3 className="car-title" id={`car-title-${index}`}>{feedback.carName}</h3>
                        <p className="order-history" id={`order-history-${index}`}>Location: {feedback.location}</p>
                        <p className="rating" id={`rating-${index}`}>Rating: ⭐⭐⭐⭐ {feedback.rating}/5</p>
                        <p className="comment" id={`comment-${index}`}>Comment: {feedback.comment}</p>
                    </div>
                ))}
            </div>
        </div>
    );
}

export default Feedback;