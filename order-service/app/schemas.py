from pydantic import BaseModel
from typing import List, Optional
from datetime import datetime

class OrderItemCreate(BaseModel):
    product_id: int
    quantity: int
    unit_price: float

class OrderCreate(BaseModel):
    user_id: int
    items: List[OrderItemCreate]

class OrderItem(OrderItemCreate):
    id: int
    order_id: int

    class Config:
        orm_mode = True

class Order(BaseModel):
    id: int
    user_id: int
    status: str
    total_amount: float
    created_at: datetime
    items: List[OrderItem]

    class Config:
        orm_mode = True

class OrderListResponse(BaseModel):
    success: bool = True
    data: list[Order]
    count: int

class Token(BaseModel):
    access_token: str
    token_type: str

class TokenData(BaseModel):
    user_id: Optional[int] = None