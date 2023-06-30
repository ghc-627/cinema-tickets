package uk.gov.uc.pairtest;

import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import thirdparty.paymentgateway.TicketPaymentServiceImpl;
import thirdparty.seatbooking.SeatReservationServiceImpl;
import uk.gov.dwp.uc.pairtest.TicketServiceImpl;
import uk.gov.dwp.uc.pairtest.domain.TicketTypeRequest;
import uk.gov.dwp.uc.pairtest.exception.InvalidAccountException;
import uk.gov.dwp.uc.pairtest.exception.InvalidNumberOfTicketsException;
import uk.gov.dwp.uc.pairtest.exception.UnaccompaniedTicketsException;


public class TestTicketService {
    @Mock
    private TicketPaymentServiceImpl ticketPaymentService;

    @Mock
    private SeatReservationServiceImpl seatReservationService;

    @InjectMocks
    private TicketServiceImpl ticketService;

    @Captor
    private ArgumentCaptor<Integer> paymentAmountCaptor;
    @Captor
    private ArgumentCaptor<Integer> seatsCaptor;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        doNothing().when(ticketPaymentService).makePayment(anyLong(), paymentAmountCaptor.capture());
        doNothing().when(seatReservationService).reserveSeat(anyLong(), seatsCaptor.capture());
    }

    /* ==================================================================== */

    // Ticket prices

    @Test
    public void testPurchaseTickets_AdultTicketCorrectPrice() {
        // The ticket prices are based on the type of ticket: Infant == £0, Child == £10, Adult == £20
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        int expected = 2000;
        ticketService.purchaseTickets((long)1, request);
        Assert.assertEquals(expected, paymentAmountCaptor.getValue().intValue());
    }

    @Test
    public void testPurchaseTickets_ChildTicketCorrectPrice() {
        // The ticket prices are based on the type of ticket: Infant == £0, Child == £10, Adult == £20
        // Cannot buy infant or child tickets without adult.
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        ticketService.purchaseTickets((long)1, adultRequest, request);
        int expected = 3000; // Adult + Child
        Assert.assertEquals(expected, paymentAmountCaptor.getValue().intValue());
    }

    @Test
    public void testPurchaseTickets_InfantTicketCorrectPrice() {
        // The ticket prices are based on the type of ticket: Infant == £0, Child == £10, Adult == £20
        // Cannot buy infant or child tickets without adult.
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
        ticketService.purchaseTickets((long)1, adultRequest, request);
        int expected = 2000; // Adult only
        Assert.assertEquals(expected, paymentAmountCaptor.getValue().intValue());
    }

    @Test
    public void testPurchaseTickets_MultipleTicketsSingleRequest() {
        // Multiple tickets can be purchased at any given time
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 3);
        int expected = 6000; // 3 x Adult
        ticketService.purchaseTickets((long)1, request);
        Assert.assertEquals(expected, paymentAmountCaptor.getValue().intValue());
    }

    @Test
    public void testPurchaseTickets_MultipleTicketsMultipleRequest() {
        // Multiple tickets can be purchased at any given time
        TicketTypeRequest requestA = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest requestB = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest requestC = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        int expected = 6000; // Adult + Child + Infant
        ticketService.purchaseTickets((long)1, requestA, requestB, requestC);
        Assert.assertEquals(expected, paymentAmountCaptor.getValue().intValue());
    }

    @Test
    public void testPurchaseTickets_MultipleTicketsMultipleRequestMultipleType() {
        // Multiple tickets can be purchased at any given time
        TicketTypeRequest requestA = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest requestB = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        int expected = 6000; // 2 x Adult + 2 x Child
        ticketService.purchaseTickets((long)1, requestA, requestB);
        Assert.assertEquals(expected, paymentAmountCaptor.getValue().intValue());
    }

    @Test
    public void testPurchaseTickets_MultipleTicketsMultipleRequestMultipleTypeIncludeInfant() {
        // Multiple tickets can be purchased at any given time
        TicketTypeRequest requestA = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest requestB = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        TicketTypeRequest requestC = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);
        int expected = 6000; // 2 x Adult + 2 x Child + 2 x Infant
        ticketService.purchaseTickets((long)1, requestA, requestB, requestC);
        Assert.assertEquals(expected, paymentAmountCaptor.getValue().intValue());
    }

    /* ==================================================================== */

    // Reservations

    @Test
    public void testPurchaseTickets_AdultReservations() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        ticketService.purchaseTickets((long)1, adultRequest);
        int expected = 2;
        Assert.assertEquals(expected, seatsCaptor.getValue().intValue());
    }

    @Test
    public void testPurchaseTickets_AdultAndChildReservations() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        ticketService.purchaseTickets((long)1, adultRequest, childRequest);
        int expected = 4;
        Assert.assertEquals(expected, seatsCaptor.getValue().intValue());
    }

    @Test
    public void testPurchaseTickets_AdultAndInfantReservations() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);
        ticketService.purchaseTickets((long)1, adultRequest, infantRequest);
        int expected = 1;
        Assert.assertEquals(expected, seatsCaptor.getValue().intValue());
    }

    @Test
    public void testPurchaseTickets_AdultChildInfantReservations() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 2);
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
        ticketService.purchaseTickets((long)1, adultRequest, childRequest, infantRequest);
        int expected = 3;
        Assert.assertEquals(expected, seatsCaptor.getValue().intValue());
    }

    @Test
    public void testPurchaseTickets_NoTicketNoReservation() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 0);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 0);
        ticketService.purchaseTickets((long)1, adultRequest, childRequest, infantRequest);
        int expected = 0;
        Assert.assertEquals(expected, seatsCaptor.getValue().intValue());
    }

    @Test
    public void testPurchaseTickets_NoRequestsNoReservations() {
        ticketService.purchaseTickets((long)1);
        int expected = 0;
        Assert.assertEquals(expected, seatsCaptor.getValue().intValue());
    }

    /* ==================================================================== */

    // Numbers of tickets

    @Test
    public void testPurchaseTickets_NoRequestsNoPayment() {
        // Only a maximum of 20 tickets that can be purchased at a time
        //TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 20);
        ticketService.purchaseTickets((long)1);
        int expected = 0;
        Assert.assertEquals(expected, paymentAmountCaptor.getValue().intValue());
    }

    @Test
    public void testPurchaseTickets_Maximum20TicketsSingleRequest() {
        // Only a maximum of 20 tickets that can be purchased at a time
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 20);
        ticketService.purchaseTickets((long)1, request);
    }

    @Test
    public void testPurchaseTickets_Maximum20TicketsMultipleRequest() {
        // Only a maximum of 20 tickets that can be purchased at a time
        TicketTypeRequest [] requests = new TicketTypeRequest[20];
        for (int i = 0; i < 20; i++) {
            requests[i] = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        }
        Assert.assertEquals(20, requests.length);
        ticketService.purchaseTickets((long)1, requests);
        int expected = 40000;
        Assert.assertEquals(expected, paymentAmountCaptor.getValue().intValue());
    }

    @Test(expected = InvalidNumberOfTicketsException.class)
    public void testPurchaseTickets_TooManyTicketsSingleRequest() {
        // Business rule: Only a maximum of 20 tickets that can be purchased at a time
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 21);
        ticketService.purchaseTickets((long)1, request);
    }

    @Test(expected = InvalidNumberOfTicketsException.class)
    public void testPurchaseTickets_TooManyTicketsMultipleRequests() {
        // Business rule: Only a maximum of 20 tickets that can be purchased at a time
        TicketTypeRequest [] requests = new TicketTypeRequest[21];
        for (int i = 0; i < 21; i++) {
            requests[i] = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        }
        Assert.assertEquals(21, requests.length);
        ticketService.purchaseTickets((long)1, requests);
    }

    @Test(expected = InvalidNumberOfTicketsException.class)
    public void testPurchaseTickets_NegativeTickets() {
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, -1);
        ticketService.purchaseTickets((long)1, request);
    }

    @Test
    public void testPurchaseTickets_ZeroTicketsGeneratesZeroPayment() {
        // Zero tickets are not explicitly excluded by the business rules
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);
        ticketService.purchaseTickets((long)1, request);
        int expected = 0;
        Assert.assertEquals(expected, paymentAmountCaptor.getValue().intValue());
    }

    /* ==================================================================== */

    // Infant exclusions

    @Test
    public void testPurchaseTickets_InfantsNotCharged() {
        // Infants do not pay for a ticket and are not allocated a seat. They will be sitting on an Adult's lap
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
        ticketService.purchaseTickets((long)1, adultRequest, request);
        int expected = 2000;
        Assert.assertEquals(expected, paymentAmountCaptor.getValue().intValue());
    }

    @Test
    public void testPurchaseTickets_InfantsSeatNotReserved() {
        // Infants do not pay for a ticket and are not allocated a seat. They will be sitting on an Adult's lap
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
        ticketService.purchaseTickets((long)1, adultRequest, request);
        int expected = 1;
        Assert.assertEquals(expected, seatsCaptor.getValue().intValue());
    }

    /* ==================================================================== */

    // Accompaniment

    @Test(expected = UnaccompaniedTicketsException.class)
    public void testPurchaseTickets_ChildRequiresAdult() {
        // Child and Infant tickets cannot be purchased without Adult tickets
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 1);
        ticketService.purchaseTickets((long)1, childRequest);
    }

    @Test(expected = UnaccompaniedTicketsException.class)
    public void testPurchaseTickets_InfantRequiresAdult() {
        // Child and Infant tickets cannot be purchased without Adult tickets
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
        ticketService.purchaseTickets((long)1, infantRequest);
    }

    @Test(expected = UnaccompaniedTicketsException.class)
    public void testPurchaseTickets_InfantRequiresAdultNonZero() {
        // Child and Infant tickets cannot be purchased without Adult tickets
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 1);
        ticketService.purchaseTickets((long)1, infantRequest, adultRequest);
    }


    @Test(expected = UnaccompaniedTicketsException.class)
    public void testPurchaseTickets_ChildAndInfantWithZeroAdultTickets() {
        TicketTypeRequest adultRequest = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 0);
        TicketTypeRequest childRequest = new TicketTypeRequest(TicketTypeRequest.Type.CHILD, 2);
        TicketTypeRequest infantRequest = new TicketTypeRequest(TicketTypeRequest.Type.INFANT, 2);
        ticketService.purchaseTickets((long)1, adultRequest, childRequest, infantRequest);
    }

    /* ==================================================================== */

    // Account assumption

    @Test(expected = InvalidAccountException.class)
    public void testPurchaseTickets_InvalidAccountId() {
        // Assumption: All accounts with an id greater than zero are valid.
        TicketTypeRequest request = new TicketTypeRequest(TicketTypeRequest.Type.ADULT, 1);
        ticketService.purchaseTickets((long)-1, request);
    }
}