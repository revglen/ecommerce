from sqlalchemy import Column, Integer, String, Float, Boolean, DateTime, func
from app.database import Base

class Product(Base):
    __tablename__ = "products"

    id=Column(Integer, primary_key=True, index=True)
    name=Column(String(255), index=True, nullable=False)
    description=Column(String(1000))
    price=Column(Float, nullable=False)
    category=Column(String(100), index=True)
    stock=Column(Integer, default=0)
    is_active=Column(Boolean, default=True)
    created_at = Column(DateTime(timezone=True), server_default=func.now())
    updated_at = Column(DateTime(timezone=True), onupdate=func.now())

    def __repr__(self):
        return f"<Product {self.name}: {self.id})>"
